package org.librarysimplified.ci.check_commits_since;

import java.time.Instant;
import java.util.Objects;

public final class CheckCommitsSinceStatusPlainFormatter
  implements CheckCommitsSinceStatusFormatterType
{
  public CheckCommitsSinceStatusPlainFormatter()
  {

  }

  /**
   * @return The name of the formatter.
   */

  public static String formatterName()
  {
    return "plain";
  }

  @Override
  public String name()
  {
    return formatterName();
  }

  @Override
  public String failed(
    final String project,
    final String branch,
    final int commits,
    final String tagName,
    final Instant tagTime)
  {
    Objects.requireNonNull(project, "project");
    Objects.requireNonNull(branch, "branch");
    Objects.requireNonNull(tagName, "tagName");
    Objects.requireNonNull(tagTime, "tagTime");

    return String.format(
      "%d commits have been made on branch '%s' since the last tag '%s' on %s. Make a new release!\n",
      Integer.valueOf(commits),
      branch,
      tagName,
      tagTime
    );
  }
}
