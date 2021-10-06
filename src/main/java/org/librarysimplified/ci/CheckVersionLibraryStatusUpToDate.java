package org.librarysimplified.ci;

import java.util.Objects;

public final class CheckVersionLibraryStatusUpToDate
  implements CheckVersionLibraryStatusType
{
  private final CheckVersionLibrary library;

  public CheckVersionLibraryStatusUpToDate(
    final CheckVersionLibrary library)
  {
    this.library =
      Objects.requireNonNull(library, "library");
  }

  @Override
  public CheckVersionLibrary library()
  {
    return this.library;
  }

  @Override
  public boolean isOk()
  {
    return true;
  }

  @Override
  public String message()
  {
    return "OK - Up to date";
  }
}
