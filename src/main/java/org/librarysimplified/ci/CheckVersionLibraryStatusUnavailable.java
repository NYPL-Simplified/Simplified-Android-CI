package org.librarysimplified.ci;

import java.util.Objects;

public final class CheckVersionLibraryStatusUnavailable
  implements CheckVersionLibraryStatusType
{
  private final CheckVersionLibrary library;

  public CheckVersionLibraryStatusUnavailable(
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
    return false;
  }

  @Override
  public String message()
  {
    return "ERROR: None of the repositories contain this artifact";
  }
}
