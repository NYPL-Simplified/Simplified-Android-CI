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
