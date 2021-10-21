package org.librarysimplified.ci.check_commits_since;

import com.beust.jcommander.Parameter;
import org.librarysimplified.ci.check_versions.CheckVersionStatusPlainFormatter;

import java.nio.file.Path;

/**
 * The structure used to hold command-line parameters.
 */

public final class CheckCommitsSinceParameters
{
  @Parameter(
    required = true,
    names = "--project",
    description = "The project name")
  String projectName;

  @Parameter(
    required = true,
    names = "--branch",
    description = "The branch to check")
  String branchName;

  @Parameter(
    required = true,
    names = "--repository",
    description = "The path to the git repository")
  Path gitRepository;

  @Parameter(
    required = false,
    names = "--releaseDaysMaximum",
    description = "The maximum number of days between releases"
  )
  int releaseDays = 14;

  @Parameter(
    required = false,
    names = "--releaseCommitCount",
    description = "The minimum number of commits for development work to be considered ready for release"
  )
  int commitCount = 2;

  @Parameter(
    required = false,
    names = "--formatter",
    description = "The formatter to use to display results")
  String formatterName = CheckVersionStatusPlainFormatter.formatterName();

  public CheckCommitsSinceParameters()
  {

  }
}
