package org.librarysimplified.ci.check_versions;

import java.util.Objects;

public final class CheckVersionLibraryStatusIgnored
  implements CheckVersionLibraryStatusType
{
  private final CheckVersionLibrary library;

  public CheckVersionLibraryStatusIgnored(
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
    return "Ignored (not included in library list file)";
  }
}
