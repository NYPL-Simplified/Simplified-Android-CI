/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

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
