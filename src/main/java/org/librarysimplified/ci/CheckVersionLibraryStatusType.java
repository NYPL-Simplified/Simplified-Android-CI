package org.librarysimplified.ci;

/**
 * The status of a check for a given library.
 */

public interface CheckVersionLibraryStatusType
  extends Comparable<CheckVersionLibraryStatusType>
{
  CheckVersionLibrary library();

  boolean isOk();

  String message();

  @Override
  default int compareTo(
    final CheckVersionLibraryStatusType other)
  {
    return this.library().compareTo(other.library());
  }
}
