package org.librarysimplified.ci;

import org.librarysimplified.ci.check_commits_since.CheckCommitsSince;
import org.librarysimplified.ci.check_versions.CheckVersions;

import java.util.Arrays;

import static java.lang.System.err;
import static java.lang.System.exit;

public final class Main
{
  private Main()
  {

  }

  private static void usage()
  {
    err.println("usage:");
    err.println("  check-versions [args]");
    err.println("  check-commits-since [args]");
  }

  private static void configureLogging()
  {
    System.setProperty(
      "java.util.logging.ConsoleHandler.formatter",
      "java.util.logging.SimpleFormatter");
    System.setProperty(
      "java.util.logging.SimpleFormatter.format",
      "%4$s: %5$s %n");
  }

  public static void main(
    final String[] args)
    throws Exception
  {
    try {
      configureLogging();

      if (args.length == 0) {
        usage();
        exit(1);
      }

      switch (args[0]) {
        case "check-versions": {
          CheckVersions.main(Arrays.copyOfRange(args, 1, args.length));
          break;
        }
        case "check-commits-since": {
          CheckCommitsSince.main(Arrays.copyOfRange(args, 1, args.length));
          break;
        }
        default: {
          usage();
          throw new ExitException(1);
        }
      }
    } catch (final ExitException e) {
      exit(e.exitCode());
    }
  }
}
