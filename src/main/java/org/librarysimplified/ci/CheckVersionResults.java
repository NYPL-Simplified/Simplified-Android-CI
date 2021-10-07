package org.librarysimplified.ci;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CheckVersionResults
{
  private final List<CheckVersionLibraryStatusType> statuses;
  private final List<CheckVersionLibraryStatusType> failed;
  private final List<CheckVersionLibraryStatusType> ignored;

  public CheckVersionResults(
    final List<CheckVersionLibraryStatusType> inStatuses)
  {
    this.statuses =
      List.copyOf(Objects.requireNonNull(inStatuses, "statuses"));

    this.failed =
      this.statuses.stream()
        .filter(s -> !s.isOk())
        .sorted()
        .collect(Collectors.toUnmodifiableList());

    this.ignored =
      this.statuses.stream()
        .filter(s -> s.library().isIgnored())
        .collect(Collectors.toUnmodifiableList());
  }

  public List<CheckVersionLibraryStatusType> statuses()
  {
    return this.statuses;
  }

  public List<CheckVersionLibraryStatusType> failed()
  {
    return this.failed;
  }

  public List<CheckVersionLibraryStatusType> ignored()
  {
    return this.ignored;
  }
}
