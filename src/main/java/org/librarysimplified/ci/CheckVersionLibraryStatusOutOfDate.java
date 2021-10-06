package org.librarysimplified.ci;

import java.util.Objects;

public final class CheckVersionLibraryStatusOutOfDate
  implements CheckVersionLibraryStatusType
{
  private final CheckVersionLibrary library;
  private final String availableVersion;

  public CheckVersionLibraryStatusOutOfDate(
    final CheckVersionLibrary library,
    final String availableVersion)
  {
    this.library =
      Objects.requireNonNull(library, "library");
    this.availableVersion =
      Objects.requireNonNull(availableVersion, "availableVersion");
  }

  @Override
  public CheckVersionLibrary library()
  {
    return this.library;
  }

  @Override
  public boolean isOk()
  {
    return false;
  }

  @Override
  public String message()
  {
    return String.format(
      "ERROR: Out of date: Newer version %s is available",
      this.availableVersion);
  }
}
