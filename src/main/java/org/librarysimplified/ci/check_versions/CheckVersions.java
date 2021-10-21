package org.librarysimplified.ci.check_versions;

import com.beust.jcommander.JCommander;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.librarysimplified.ci.ExitException;
import org.tomlj.Toml;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CheckVersions
{
  private static final Logger LOG = Logger.getLogger("CheckVersions");

  private static final Map<String, CheckVersionStatusFormatterType> FORMATTERS =
    Stream.of(
      new CheckVersionStatusPlainFormatter(),
      new CheckVersionStatusSlackFormatter()
    ).collect(Collectors.toMap(
      CheckVersionStatusFormatterType::name,
      Function.identity())
    );

  private CheckVersions()
  {

  }

  public static void main(
    final String[] args)
    throws IOException, ExitException, JPropertyException
  {
    final var parameters = new CheckVersionsParameters();

    try {
      JCommander.newBuilder()
        .addObject(parameters)
        .build()
        .parse(args);
    } catch (final Exception e) {
      LOG.severe("Error parsing arguments: " + e.getMessage());
      throw new ExitException(1);
    }

    final var formatter = FORMATTERS.get(parameters.formatterName);
    if (formatter == null) {
      LOG.severe("No formatter exists with the name '" + parameters.formatterName + "'");
      LOG.severe("Existing formatters include: " + FORMATTERS.keySet());
      throw new ExitException(1);
    }

    final var config = new Properties();
    final var configPath = parameters.configurationFile.toAbsolutePath();
    try (var stream = Files.newInputStream(configPath)) {
      config.load(stream);
    }

    var versionCatalogPath =
      Paths.get(JProperties.getString(config, "versionCatalogFile"));
    versionCatalogPath =
      resolveAgainstConfigPath(configPath, versionCatalogPath);

    var libraryListFile =
      Paths.get(JProperties.getString(config, "libraryListFile"));
    libraryListFile =
      resolveAgainstConfigPath(configPath, libraryListFile);

    var libraryRepositoryFile =
      Paths.get(JProperties.getString(config, "libraryRepositoryFile"));
    libraryRepositoryFile =
      resolveAgainstConfigPath(configPath, libraryRepositoryFile);

    final Set<String> checkLibraries;
    try (var stream = Files.lines(libraryListFile)) {
      checkLibraries = nonCommentedLinesOf(stream);
    }
    final Set<String> checkRepositories;
    try (var stream = Files.lines(libraryRepositoryFile)) {
      checkRepositories = nonCommentedLinesOf(stream);
    }

    if (checkRepositories.isEmpty()) {
      LOG.severe("No repositories were provided. This seems like a mistake.");
      throw new ExitException(1);
    }
    if (checkLibraries.isEmpty()) {
      LOG.severe("No libraries were provided. This seems like a mistake.");
      throw new ExitException(1);
    }

    final var librariesToCheck =
      parseLibraries(versionCatalogPath, checkLibraries, checkRepositories);
    final var results =
      performAllChecks(librariesToCheck);

    System.out.println(formatter.format(results));

    if (results.ignored().size() == results.statuses().size()) {
      LOG.severe("All libraries were ignored. This seems like a mistake!");
      throw new ExitException(1);
    }

    if (!results.failed().isEmpty()) {
      throw new ExitException(1);
    }
  }

  private static Set<String> nonCommentedLinesOf(
    final Stream<String> stream)
  {
    return stream.filter(line -> !line.startsWith("#"))
      .filter(line -> !line.isBlank())
      .map(String::trim)
      .collect(Collectors.toSet());
  }

  private static Path resolveAgainstConfigPath(
    final Path configPath,
    final Path versionCatalogPath)
  {
    if (!versionCatalogPath.isAbsolute()) {
      return configPath.getParent()
        .resolve(versionCatalogPath)
        .toAbsolutePath();
    }
    return versionCatalogPath;
  }

  private static CheckVersionResults performAllChecks(
    final Collection<CheckVersionLibrary> librariesToCheck)
  {
    final var client =
      HttpClient.newHttpClient();

    final var statuses =
      new ArrayList<CheckVersionLibraryStatusType>(librariesToCheck.size());

    for (final var library : librariesToCheck) {
      statuses.add(library.check(client));
    }

    return new CheckVersionResults(statuses);
  }

  private static ArrayList<CheckVersionLibrary> parseLibraries(
    final Path librariesPath,
    final Collection<String> checkLibraries,
    final Set<String> checkRepositories)
    throws IOException, ExitException
  {
    final var librariesResult =
      Toml.parse(librariesPath);

    if (librariesResult.hasErrors()) {
      LOG.severe("The TOML file " + librariesPath + " contained parse errors.");
      librariesResult.errors().forEach(error -> LOG.severe(error.toString()));
      throw new ExitException(1);
    }

    final var libraries =
      librariesResult.getTable("libraries");
    final var versions =
      librariesResult.getTable("versions");

    final var librariesToCheck =
      new ArrayList<CheckVersionLibrary>();

    final var namesLeftOver = new HashSet<>(checkLibraries);
    for (final var libraryName : libraries.keySet()) {
      librariesToCheck.add(
        parseLibrary(
          checkLibraries,
          checkRepositories,
          libraries,
          versions,
          namesLeftOver,
          libraryName)
      );
    }

    if (!namesLeftOver.isEmpty()) {
      LOG.severe(
        "The library list contains names that do not appear in the build versions TOML file: " + namesLeftOver);
      throw new ExitException(1);
    }

    return librariesToCheck;
  }

  private static CheckVersionLibrary parseLibrary(
    final Collection<String> checkLibraries,
    final Set<String> checkRepositories,
    final TomlTable libraries,
    final TomlTable versions,
    final Set<String> namesLeftOver,
    final String libraryName)
  {
    final var module =
      libraries.getTable(libraryName);
    final var name =
      module.getString("module");

    final var versionRef =
      module.getString("version.ref");
    final var version =
      versions.getString(versionRef);
    final var group =
      name.substring(0, name.indexOf(':'));
    final var artifact =
      name.substring(name.indexOf(':') + 1);

    final boolean shouldIgnore;
    if (checkLibraries.contains(name)) {
      namesLeftOver.remove(name);
      shouldIgnore = false;
    } else {
      shouldIgnore = true;
    }

    return new CheckVersionLibrary(
      group,
      artifact,
      new DefaultArtifactVersion(version),
      checkRepositories,
      shouldIgnore
    );
  }
}
