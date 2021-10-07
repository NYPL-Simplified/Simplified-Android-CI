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

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;

import java.util.Arrays;
import java.util.Objects;

public final class CheckVersionStatusPlainFormatter
  implements CheckVersionStatusFormatterType
{
  public CheckVersionStatusPlainFormatter()
  {

  }

  /**
   * @return The name of the formatter.
   */

  public static String formatterName()
  {
    return "plain";
  }

  private static void checkedFooter(
    final CheckVersionResults results,
    final int notIgnored,
    final StringBuilder message)
  {
    message.append(notIgnored);
    message.append(" libraries were checked. ");
    message.append((long) results.ignored().size());
    message.append(" libraries were ignored.");
  }

  @Override
  public String format(
    final CheckVersionResults results)
  {
    Objects.requireNonNull(results, "results");

    final var notIgnored =
      results.statuses().size() - results.ignored().size();

    final var message = new StringBuilder(1024);

    if (!results.failed().isEmpty()) {
      message.append(
        "At least one library is out-of-date, or failed the dependency check!");
      message.append(System.lineSeparator());
      message.append(System.lineSeparator());
      message.append(
        AsciiTable.getTable(
          results.failed(),
          Arrays.asList(
            new Column().header("Group").with(s -> s.library().group()),
            new Column().header("Artifact").with(s -> s.library().artifact()),
            new Column().header("Version").with(s -> s.library().version().toString()),
            new Column().header("Message").with(CheckVersionLibraryStatusType::message)
          )
        ));

      message.append(System.lineSeparator());
      message.append(System.lineSeparator());
      checkedFooter(results, notIgnored, message);
      return message.toString();
    }

    message.append("All of the checked libraries are up-to-date.");
    message.append(System.lineSeparator());
    checkedFooter(results, notIgnored, message);
    return message.toString();
  }

  @Override
  public String name()
  {
    return formatterName();
  }
}
