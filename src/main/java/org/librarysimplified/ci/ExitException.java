package org.librarysimplified.ci;

/**
 * An exception indicating that the program wants to quit with the given
 * exit code. Present because code that calls System.exit() directly is hard
 * to test.
 */

public final class ExitException
  extends Exception
{
  private final int exitCode;

  public ExitException(
    final int exitCode)
  {
    this.exitCode = exitCode;
  }

  public int exitCode()
  {
    return this.exitCode;
  }
}
