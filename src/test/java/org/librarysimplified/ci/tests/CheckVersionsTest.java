package org.librarysimplified.ci.tests;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.librarysimplified.ci.CheckVersions;
import org.librarysimplified.ci.ExitException;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.librarysimplified.ci.tests.TestDirectories.resourceBytesOf;
import static org.librarysimplified.ci.tests.TestDirectories.resourceOf;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public final class CheckVersionsTest
{
  private Path directory;
  private Path tomlPath;
  private Path libraryListPath;
  private Path libraryRepositoryListPath;
  private Path configPath;
  private ClientAndServer server0;
  private ClientAndServer server1;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory =
      TestDirectories.createTempDirectory();

    this.configPath =
      this.directory.resolve("checkVersion.properties");
    this.tomlPath =
      this.directory.resolve("versions.toml");
    this.libraryListPath =
      this.directory.resolve("libraryList.txt");
    this.libraryRepositoryListPath =
      this.directory.resolve("libraryRepositoryList.txt");

    final var config = new Properties();
    config.setProperty(
      "versionCatalogFile", this.tomlPath.toString());
    config.setProperty(
      "libraryListFile", this.libraryListPath.toString());
    config.setProperty(
      "libraryRepositoryFile", this.libraryRepositoryListPath.toString());

    try (var output = Files.newOutputStream(this.configPath)) {
      config.store(output, "");
    }

    this.server0 =
      startClientAndServer(Integer.valueOf(10000));
    this.server1 =
      startClientAndServer(Integer.valueOf(10001));
  }

  @AfterEach
  public void tearDown()
  {
    this.server0.stop();
    this.server1.stop();
  }

  /**
   * The command-line tool fails if important files are missing.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMissingFiles0()
    throws Exception
  {
    assertThrows(NoSuchFileException.class, () -> {
      CheckVersions.main(new String[]{this.configPath.toString()});
    });
  }

  /**
   * The command-line tool fails if important files are missing.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMissingFiles1()
    throws Exception
  {
    Files.writeString(this.libraryListPath, "");

    assertThrows(NoSuchFileException.class, () -> {
      CheckVersions.main(new String[]{this.configPath.toString()});
    });
  }

  /**
   * The command-line tool fails if important files are empty.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmptyFiles0()
    throws Exception
  {
    Files.writeString(this.libraryListPath, "");
    Files.writeString(
      this.libraryRepositoryListPath,
      "https://www.example.com/");

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());
  }

  /**
   * The command-line tool fails if important files are empty.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmptyFiles1()
    throws Exception
  {
    Files.writeString(this.libraryListPath, "x:y");
    Files.writeString(this.libraryRepositoryListPath, "");

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());
  }

  /**
   * The command-line tool fails if the list of libraries tries to refer to
   * something that isn't in the TOML file.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibraryNameWrong()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(this.libraryListPath, "x:z");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/"
    );

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());
  }

  /**
   * The command-line tool fails if the TOML file is garbage.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnparseableTOMLFile()
    throws Exception
  {
    Files.writeString(this.tomlPath, "@#'!");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/"
    );

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());
  }

  /**
   * Library checks fail if artifacts are not found.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibraryNotFound()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/"
    );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(404)));

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
  }

  /**
   * Library checks fail if servers fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibraryFails()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/"
    );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(500)));

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
  }

  /**
   * Library checks fail if connections fail.
   *
   * @throws Exception On errors
   */


  @Test
  public void testLibraryConnectFails()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:20000/"
    );

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());
  }

  /**
   * Libraries that are out of date are marked as such.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibraryOutOfDate()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/"
    );

    final var versionText =
      resourceBytesOf(
        CheckVersionsTest.class,
        this.directory,
        "basicVersionResponseTooOld.xml"
      );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
  }

  /**
   * Libraries that are out of date are marked as such.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibrariesOutOfDate()
    throws Exception
  {
    this.writeVersionsFile("moreVersions.toml");

    Files.writeString(
      this.libraryListPath,
      "x:y\n" +
        "y:z\n" +
        "a:b\n");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/"
    );

    final var versionText =
      resourceBytesOf(
        CheckVersionsTest.class,
        this.directory,
        "basicVersionResponseTooOld.xml"
      );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));
    this.server0
      .when(request().withPath("/y/z/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));
    this.server0
      .when(request().withPath("/a/b/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
    this.server0.verify(request().withPath("/y/z/maven-metadata.xml"));
    this.server0.verify(request().withPath("/a/b/maven-metadata.xml"));
  }

  /**
   * Libraries that are out of date are marked as such.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibrariesOutOfDateOneIgnored()
    throws Exception
  {
    this.writeVersionsFile("moreVersions.toml");

    Files.writeString(
      this.libraryListPath,
      "x:y\n" +
        "a:b\n");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/"
    );

    final var versionText =
      resourceBytesOf(
        CheckVersionsTest.class,
        this.directory,
        "basicVersionResponseTooOld.xml"
      );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));
    this.server0
      .when(request().withPath("/a/b/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
    this.server0.verify(request().withPath("/a/b/maven-metadata.xml"));
  }

  private void writeVersionsFile(
    final String name)
    throws IOException
  {
    resourceOf(CheckVersionsTest.class, this.directory, name);
    Files.move(this.directory.resolve(name), this.tomlPath);
  }

  /**
   * Library checks fail if the server produces garbage.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibraryServesGarbage0()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/"
    );

    final var versionText =
      resourceBytesOf(
        CheckVersionsTest.class,
        this.directory,
        "basicVersionResponseNonsense0.xml"
      );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200))
                 .withBody(versionText));

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
  }

  /**
   * Library checks fail if the server produces garbage.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibraryServesGarbage1()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/"
    );

    final var versionText =
      resourceBytesOf(
        CheckVersionsTest.class,
        this.directory,
        "basicVersionResponseNonsense1.xml"
      );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
  }

  /**
   * A library is found if it exists on at least one of the repositories.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibraryOnSecondRepositoryOutOfDate()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/\n" +
        "http://127.0.0.1:10001/"
    );

    final var versionText =
      resourceBytesOf(
        CheckVersionsTest.class,
        this.directory,
        "basicVersionResponseTooOld.xml"
      );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(404)));
    this.server1
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckVersions.main(new String[]{this.configPath.toString()});
      });
    assertEquals(1, ex.exitCode());

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
    this.server1.verify(request().withPath("/x/y/maven-metadata.xml"));
  }

  /**
   * A library is found if it exists on at least one of the repositories.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibraryOK()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/\n" +
        "http://127.0.0.1:10001/"
    );

    final var versionText =
      resourceBytesOf(
        CheckVersionsTest.class,
        this.directory,
        "basicVersionResponseOK.xml"
      );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));

    CheckVersions.main(new String[]{this.configPath.toString()});

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
  }

  /**
   * A library is found if it exists on at least one of the repositories.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLibraryOnSecondRepositoryOK()
    throws Exception
  {
    this.writeVersionsFile("basicVersion.toml");

    Files.writeString(
      this.libraryListPath, "x:y");
    Files.writeString(
      this.libraryRepositoryListPath,
      "http://127.0.0.1:10000/\n" +
        "http://127.0.0.1:10001/"
    );

    final var versionText =
      resourceBytesOf(
        CheckVersionsTest.class,
        this.directory,
        "basicVersionResponseOK.xml"
      );

    this.server0
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(404)));
    this.server1
      .when(request().withPath("/x/y/maven-metadata.xml"))
      .respond(response().withStatusCode(Integer.valueOf(200)).withBody(
        versionText));

    CheckVersions.main(new String[]{this.configPath.toString()});

    this.server0.verify(request().withPath("/x/y/maven-metadata.xml"));
    this.server1.verify(request().withPath("/x/y/maven-metadata.xml"));
  }

  /**
   * Versions have the expected comparison behaviour.
   */

  @Test
  public void testVersionsCompare()
  {
    final var v0 =
      new DefaultArtifactVersion("1.0.0");
    final var v1 =
      new DefaultArtifactVersion("1.0.0-SNAPSHOT");

    assertTrue(v0.compareTo(v1) > 0);
  }
}
