package org.librarysimplified.ci.check_commits_since;

import java.time.Instant;

/**
 * The type of formatters that produce human-readable output for library
 * checks.
 */

public interface CheckCommitsSinceStatusFormatterType
{
  /**
   * @return The name of the formatter
   */

  String name();

  /**
   * Indicate that the check failed.
   *
   * @param project The project name
   * @param branch  The git branch
   * @param commits The number of commits that have been made
   * @param tagName The name of the last tag
   * @param tagTime The time of the last tag
   *
   * @return A humanly-readable string
   */

  String failed(
    String project,
    String branch,
    int commits,
    String tagName,
    Instant tagTime);
}
