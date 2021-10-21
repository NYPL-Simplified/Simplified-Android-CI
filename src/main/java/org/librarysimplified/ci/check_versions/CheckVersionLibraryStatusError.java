package org.librarysimplified.ci.check_versions;

import java.util.Objects;

public final class CheckVersionLibraryStatusError
  implements CheckVersionLibraryStatusType
{
  private final CheckVersionLibrary library;
  private final String message;

  public CheckVersionLibraryStatusError(
    final CheckVersionLibrary library,
    final String message)
  {
    this.library =
      Objects.requireNonNull(library, "library");
    this.message =
      Objects.requireNonNull(message, "message");
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
    return "ERROR: " + this.message;
  }
}
