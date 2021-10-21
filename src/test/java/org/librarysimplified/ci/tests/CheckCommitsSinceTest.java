package org.librarysimplified.ci.tests;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevObject;
import org.joda.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.librarysimplified.ci.ExitException;
import org.librarysimplified.ci.check_commits_since.CheckCommitsSince;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.joda.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class CheckCommitsSinceTest
{
  private static final Logger LOG = Logger.getLogger("CheckCommitsSinceTest");

  private Path directory;
  private Path directoryGit;

  private static void configureLogging()
  {
    System.setProperty(
      "java.util.logging.ConsoleHandler.formatter",
      "java.util.logging.SimpleFormatter");
    System.setProperty(
      "java.util.logging.SimpleFormatter.format",
      "%4$s: %5$s %n");
  }

  @BeforeEach
  public void setup()
    throws IOException
  {
    configureLogging();

    this.directory =
      TestDirectories.createTempDirectory();
    this.directoryGit =
      this.directory.resolve(".git");
  }

  @AfterEach
  public void tearDown()
  {

  }

  /**
   * The command-line tool fails if no branch name is provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMissingConfiguration0()
    throws Exception
  {
    final var ex = assertThrows(ExitException.class, () -> {
      CheckCommitsSince.main(new String[]{

      });
    });
    assertEquals(1, ex.exitCode());
  }

  /**
   * The command-line tool fails if no repository name is provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMissingConfiguration1()
    throws Exception
  {
    final var ex = assertThrows(ExitException.class, () -> {
      CheckCommitsSince.main(new String[]{
        "--project",
        "Somewhere/Else",
        "--branch",
        "develop"
      });
    });
    assertEquals(1, ex.exitCode());
  }

  /**
   * The command-line tool fails if no project name is provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMissingConfiguration2()
    throws Exception
  {
    final var ex = assertThrows(ExitException.class, () -> {
      CheckCommitsSince.main(new String[]{
        "--project",
        "Somewhere/Else",
        "--repository",
        this.directoryGit.toString()
      });
    });
    assertEquals(1, ex.exitCode());
  }

  /**
   * The command-line tool fails if an invalid formatter name is provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNonsenseConfiguration0()
    throws Exception
  {
    final var ex = assertThrows(ExitException.class, () -> {
      CheckCommitsSince.main(new String[]{
        "--project",
        "Somewhere/Else",
        "--repository",
        this.directoryGit.toString(),
        "--branch",
        "develop",
        "--formatter",
        "What?"
      });
    });
    assertEquals(1, ex.exitCode());
  }

  /**
   * An empty repository doesn't contain the named branch.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmptyRepository()
    throws Exception
  {
    try (var git = Git.init()
      .setDirectory(this.directory.toFile())
      .setGitDir(this.directoryGit.toFile())
      .call()) {
      LOG.info("created " + this.directoryGit);
    }

    CheckCommitsSince.main(new String[]{
      "--project",
      "Somewhere/Else",
      "--branch",
      "develop",
      "--repository",
      this.directoryGit.toString()
    });
  }

  /**
   * Too much time has passed since the last release.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckTooLong()
    throws Exception
  {
    try (var git = Git.init()
      .setDirectory(this.directory.toFile())
      .setGitDir(this.directoryGit.toFile())
      .setInitialBranch("develop")
      .call()) {
      LOG.info("created " + this.directoryGit);
      commitOnDate(git, date("2021-01-01T00:00:00Z"), "Message 0");
      commitOnDate(git, date("2021-01-02T00:00:00Z"), "Message 1");

      final var commit0 =
        commitOnDate(git, date("2021-01-03T00:00:00Z"), "Message 2");
      tagOnDate(git, date("2021-01-04T00:00:00Z"), commit0, "tag0");

      commitOnDate(git, date("2021-01-05T00:00:00Z"), "Message 3");
      commitOnDate(git, date("2021-01-06T00:00:00Z"), "Message 4");
      commitOnDate(git, date("2021-01-07T00:00:00Z"), "Message 5");
      commitOnDate(git, date("2021-01-08T00:00:00Z"), "Message 6");

      final var commit1 =
        commitOnDate(git, date("2021-01-09T00:00:00Z"), "Message 7");
      tagOnDate(git, date("2021-01-10T00:00:00Z"), commit1, "tag1");

      commitOnDate(git, date("2021-01-11T00:00:00Z"), "Message 8");
      commitOnDate(git, date("2021-01-12T00:00:00Z"), "Message 9");
    }

    final var ex =
      assertThrows(ExitException.class, () -> {
        CheckCommitsSince.main(new String[]{
          "--project",
          "Somewhere/Else",
          "--branch",
          "develop",
          "--repository",
          this.directoryGit.toString(),
          "--releaseDaysMaximum",
          "14",
          "--releaseCommitCount",
          "2"
        });
      });
    assertEquals(1, ex.exitCode());
  }

  /**
   * Many commits have been made since the last tag, but the required time
   * period hasn't elapsed yet.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckTooManyCommitsButNoTimeElapsed()
    throws Exception
  {
    try (var git = Git.init()
      .setDirectory(this.directory.toFile())
      .setGitDir(this.directoryGit.toFile())
      .setInitialBranch("develop")
      .call()) {
      LOG.info("created " + this.directoryGit);
      commitOnDate(git, now(), "Message 0");
      commitOnDate(git, now(), "Message 1");

      final var commit0 =
        commitOnDate(git, now(), "Message 2");
      tagOnDate(git, now(), commit0, "tag0");

      commitOnDate(git, now(), "Message 3");
      commitOnDate(git, now(), "Message 4");
      commitOnDate(git, now(), "Message 5");
      commitOnDate(git, now(), "Message 6");
      commitOnDate(git, now(), "Message 7");
      commitOnDate(git, now(), "Message 8");
      commitOnDate(git, now(), "Message 9");
    }

    CheckCommitsSince.main(new String[]{
      "--project",
      "Somewhere/Else",
      "--branch",
      "develop",
      "--repository",
      this.directoryGit.toString(),
      "--releaseDaysMaximum",
      "14",
      "--releaseCommitCount",
      "2"
    });
  }

  /**
   * Weeks have passed since the last release, but no commits have been made.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckLongButNoCommits()
    throws Exception
  {
    try (var git = Git.init()
      .setDirectory(this.directory.toFile())
      .setGitDir(this.directoryGit.toFile())
      .setInitialBranch("develop")
      .call()) {
      LOG.info("created " + this.directoryGit);
      commitOnDate(git, date("2021-01-01T00:00:00Z"), "Message 0");
      commitOnDate(git, date("2021-01-02T00:00:00Z"), "Message 1");

      final var commit0 =
        commitOnDate(git, date("2021-01-03T00:00:00Z"), "Message 2");
      tagOnDate(git, date("2021-01-04T00:00:00Z"), commit0, "tag0");

      commitOnDate(git, date("2021-01-05T00:00:00Z"), "Message 3");
      commitOnDate(git, date("2021-01-06T00:00:00Z"), "Message 4");
      commitOnDate(git, date("2021-01-07T00:00:00Z"), "Message 5");
      commitOnDate(git, date("2021-01-08T00:00:00Z"), "Message 6");

      final var commit1 =
        commitOnDate(git, date("2021-01-09T00:00:00Z"), "Message 7");
      tagOnDate(git, date("2021-01-10T00:00:00Z"), commit1, "tag1");
    }

    CheckCommitsSince.main(new String[]{
      "--project",
      "Somewhere/Else",
      "--branch",
      "develop",
      "--repository",
      this.directoryGit.toString(),
      "--releaseDaysMaximum",
      "14",
      "--releaseCommitCount",
      "2"
    });
  }

  private static Instant date(
    final String text)
  {
    return Instant.parse(text);
  }

  private static void tagOnDate(
    final Git git,
    final Instant date,
    final RevObject object,
    final String tag)
    throws GitAPIException
  {
    final var defaultCommitter =
      new PersonIdent(git.getRepository());
    final var committer =
      new PersonIdent(defaultCommitter, date.toDate());

    final var tagR =
      git.tag()
        .setMessage(tag)
        .setName(tag)
        .setSigned(false)
        .setTagger(committer)
        .setObjectId(object)
        .call();

    LOG.info(String.format("tag %s", tagR.getName()));
  }

  private static RevObject commitOnDate(
    final Git git,
    final Instant date,
    final String message)
    throws GitAPIException
  {
    final var defaultCommitter =
      new PersonIdent(git.getRepository());
    final var committer =
      new PersonIdent(defaultCommitter, date.toDate());

    final var ref =
      git.commit()
        .setMessage(message)
        .setSign(Boolean.FALSE)
        .setAllowEmpty(true)
        .setCommitter(committer)
        .call();

    LOG.info(String.format("commit %s", ref.getName()));
    return ref;
  }
}
