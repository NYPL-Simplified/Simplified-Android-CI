package org.librarysimplified.ci.check_versions;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.Objects;

public final class CheckVersionLibraryStatusOutOfDate
  implements CheckVersionLibraryStatusType
{
  private final CheckVersionLibrary library;
  private final DefaultArtifactVersion availableVersion;

  public CheckVersionLibraryStatusOutOfDate(
    final CheckVersionLibrary library,
    final DefaultArtifactVersion availableVersion)
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
      "Out of date: Newer version %s is available",
      this.availableVersion);
  }
}
