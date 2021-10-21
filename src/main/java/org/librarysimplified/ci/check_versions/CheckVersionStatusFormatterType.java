package org.librarysimplified.ci.check_versions;

/**
 * The type of formatters that produce human-readable output for library
 * checks.
 */

public interface CheckVersionStatusFormatterType
{
  /**
   * @return The name of the formatter
   */

  String name();

  /**
   * Format the given results into something readable.
   *
   * @param results The results
   *
   * @return A humanly-readable string
   */

  String format(CheckVersionResults results);
}
