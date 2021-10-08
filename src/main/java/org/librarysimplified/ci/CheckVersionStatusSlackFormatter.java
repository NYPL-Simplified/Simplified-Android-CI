package org.librarysimplified.ci;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CheckVersionStatusSlackFormatter
  implements CheckVersionStatusFormatterType
{
  public CheckVersionStatusSlackFormatter()
  {

  }

  /**
   * @return The name of the formatter.
   */

  public static String formatterName()
  {
    return "slack";
  }

  private static ObjectNode generateOKHeader(
    final ObjectMapper mapper)
  {
    final var textSection = mapper.createObjectNode();
    textSection.put("type", "mrkdwn");
    textSection.put(
      "text",
      ":white_check_mark:    All of the checked libraries are up-to-date.");

    final var section = mapper.createObjectNode();
    section.put("type", "section");
    section.set("text", textSection);
    return section;
  }

  private static ObjectNode generateFooter(
    final ObjectMapper mapper,
    final CheckVersionResults results)
  {
    final var notIgnored =
      results.statuses().size() - results.ignored().size();

    final var text = new StringBuilder(128);
    text.append(notIgnored);
    text.append(" libraries were checked. ");
    text.append((long) results.ignored().size());
    text.append(" libraries were ignored.");

    final var textSection = mapper.createObjectNode();
    textSection.put("type", "mrkdwn");
    textSection.put("text", text.toString());

    final var section = mapper.createObjectNode();
    section.put("type", "section");
    section.set("text", textSection);
    return section;
  }

  private static ObjectNode generateErrorTable(
    final ObjectMapper mapper,
    final List<CheckVersionLibraryStatusType> failed)
  {
    final var headerLibrary = mapper.createObjectNode();
    headerLibrary.put("type", "mrkdwn");
    headerLibrary.put("text", "Library");

    final var headerStatus = mapper.createObjectNode();
    headerStatus.put("type", "mrkdwn");
    headerStatus.put("text", "Status");

    final var fields = mapper.createArrayNode();
    fields.add(headerLibrary);
    fields.add(headerStatus);

    /*
     * Slack limits fields to ten (with two used for the headers).
     */

    final var firsts =
      failed.stream()
        .limit(4L)
        .collect(Collectors.toList());

    for (final var status : firsts) {
      final var artifactHolder = mapper.createObjectNode();
      final var statusHolder = mapper.createObjectNode();

      final var library = status.library();
      final var name = new StringBuilder(128);
      name.append('`');
      name.append(library.artifact());
      name.append(':');
      name.append(library.version());
      name.append('`');
      artifactHolder.put("type", "mrkdwn");
      artifactHolder.put("text", name.toString());

      statusHolder.put("type", "plain_text");
      statusHolder.put("text", status.message());

      fields.add(artifactHolder);
      fields.add(statusHolder);
    }

    final var section = mapper.createObjectNode();
    section.put("type", "section");
    section.set("fields", fields);
    return section;
  }

  private static ObjectNode generateErrorHeader(
    final ObjectMapper mapper,
    final List<CheckVersionLibraryStatusType> failed)
  {
    final var message = new StringBuilder(128);
    if (failed.size() > 4) {
      message.append(System.lineSeparator());
      message.append(
        ":warning:    More than four libraries are out of date; the first four are listed below.");
      message.append(
        " Please run `ci-check-versions.sh` locally to see the full list.");
    } else {
      message.append(
        ":warning:    At least one library is out-of-date, or failed the dependency check!");
    }

    final var textSection = mapper.createObjectNode();
    textSection.put("type", "mrkdwn");
    textSection.put("text", message.toString());

    final var section = mapper.createObjectNode();
    section.put("type", "section");
    section.set("text", textSection);
    return section;
  }

  @Override
  public String format(
    final CheckVersionResults results)
  {
    Objects.requireNonNull(results, "results");

    try {
      final var mapper = new ObjectMapper();
      final var blocks = mapper.createArrayNode();

      final var failed = results.failed();
      if (!failed.isEmpty()) {
        blocks.add(generateErrorHeader(mapper, failed));
        blocks.add(generateErrorTable(mapper, failed));
      } else {
        blocks.add(generateOKHeader(mapper));
      }

      blocks.add(generateFooter(mapper, results));
      final var root = mapper.createObjectNode();
      root.set("blocks", blocks);
      return mapper.writeValueAsString(root);
    } catch (final JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String name()
  {
    return formatterName();
  }
}
