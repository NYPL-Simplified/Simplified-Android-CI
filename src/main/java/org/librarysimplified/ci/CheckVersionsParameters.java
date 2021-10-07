package org.librarysimplified.ci;

import com.beust.jcommander.Parameter;

import java.nio.file.Path;

/**
 * The structure used to hold command-line parameters.
 */

public final class CheckVersionsParameters
{
  @Parameter(
    required = true,
    names = "--configuration",
    description = "The configuration file")
  Path configurationFile;

  @Parameter(
    required = false,
    names = "--formatter",
    description = "The formatter to use to display results")
  String formatterName = CheckVersionStatusPlainFormatter.formatterName();

  public CheckVersionsParameters()
  {

  }
}
