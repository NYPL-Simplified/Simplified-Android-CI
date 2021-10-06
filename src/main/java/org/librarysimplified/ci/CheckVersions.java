package org.librarysimplified.ci;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyException;
import org.tomlj.Toml;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.System.err;

public final class CheckVersions
{
  private static final Logger LOG = Logger.getLogger("CheckVersions");

  private CheckVersions()
  {

  }

  public static void main(
    final String[] args)
    throws IOException, ExitException, JPropertyException
  {
    if (args.length != 1) {
      err.println("usage: checkVersion.properties");
      throw new ExitException(1);
    }

    final var config = new Properties();
    try (var stream = Files.newInputStream(Paths.get(args[0]))) {
      config.load(stream);
    }

    final var versionCatalogPath =
      Paths.get(JProperties.getString(config, "versionCatalogFile"));
    final var libraryListFile =
      Paths.get(JProperties.getString(config, "libraryListFile"));
    final var libraryRepositoryFile =
      Paths.get(JProperties.getString(config, "libraryRepositoryFile"));

    final Set<String> checkLibraries;
    try (var stream = Files.lines(libraryListFile)) {
      checkLibraries =
        stream.filter(line -> !line.startsWith("#"))
          .filter(line -> !line.isBlank())
          .map(String::trim)
          .collect(Collectors.toSet());
    }
    final Set<String> checkRepositories;
    try (var stream = Files.lines(libraryRepositoryFile)) {
      checkRepositories =
        stream.filter(line -> !line.startsWith("#"))
          .filter(line -> !line.isBlank())
          .map(String::trim)
          .collect(Collectors.toSet());
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
    final var statuses =
      performAllChecks(librariesToCheck);
    final var ok =
      displayResults(statuses);

    final var notIgnored =
      statuses.stream()
        .filter(s -> !s.library().isIgnored())
        .count();

    if (notIgnored == 0L) {
      LOG.severe("All libraries were ignored. This seems like a mistake!");
      throw new ExitException(1);
    }

    if (ok) {
      final var message = new StringBuilder(80);
      message.append(notIgnored);
      message.append(" libraries were checked (");
      message.append(statuses.size() - notIgnored);
      message.append(" were ignored), and all checked libraries were up-to-date");
      LOG.info(message.toString());
    } else {
      LOG.severe(
        "At least one library is out-of-date or failed the check in some manner.");
      throw new ExitException(1);
    }
  }

  private static boolean displayResults(
    final List<CheckVersionLibraryStatusType> statuses)
  {
    final var failed =
      statuses.stream()
        .filter(s -> !s.isOk())
        .sorted()
        .collect(Collectors.toList());

    if (!failed.isEmpty()) {
      final var table =
        AsciiTable.getTable(failed, Arrays.asList(
          new Column().header("Group").with(s -> s.library().group()),
          new Column().header("Artifact").with(s -> s.library().artifact()),
          new Column().header("Version").with(s -> s.library().version()),
          new Column().header("Message").with(CheckVersionLibraryStatusType::message)
        ));
      System.out.println(table);
    }
    return failed.isEmpty();
  }

  private static ArrayList<CheckVersionLibraryStatusType> performAllChecks(
    final Collection<CheckVersionLibrary> librariesToCheck)
  {
    final var client =
      HttpClient.newHttpClient();

    final var statuses =
      new ArrayList<CheckVersionLibraryStatusType>(librariesToCheck.size());

    for (final var library : librariesToCheck) {
      statuses.add(library.check(client));
    }
    return statuses;
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

      librariesToCheck.add(
        new CheckVersionLibrary(
          group,
          artifact,
          version,
          checkRepositories,
          shouldIgnore
      ));
    }

    if (!namesLeftOver.isEmpty()) {
      LOG.severe(
        "The library list contains names that do not appear in the build versions TOML file: " + namesLeftOver);
      throw new ExitException(1);
    }

    return librariesToCheck;
  }
}
