package org.librarysimplified.ci.check_commits_since;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Objects;

public final class CheckCommitsSinceStatusSlackFormatter
  implements CheckCommitsSinceStatusFormatterType
{
  public CheckCommitsSinceStatusSlackFormatter()
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
    final ObjectMapper mapper,
    final String project,
    final String branch,
    final int commits,
    final String tagName,
    final Instant tagTime)
  {
    final var textSection = mapper.createObjectNode();
    textSection.put("type", "mrkdwn");
    textSection.put(
      "text",
      String.format(
        ":warning:    `%s`: %d commits have been made on branch '%s' since the last tag '%s' on %s. Make a new release!\n",
        project,
        Integer.valueOf(commits),
        branch,
        tagName,
        tagTime
      ));

    final var section = mapper.createObjectNode();
    section.put("type", "section");
    section.set("text", textSection);
    return section;
  }

  @Override
  public String name()
  {
    return formatterName();
  }

  @Override
  public String failed(
    final String project,
    final String branch,
    final int commits,
    final String tagName,
    final Instant tagTime)
  {
    Objects.requireNonNull(project, "project");
    Objects.requireNonNull(branch, "branch");
    Objects.requireNonNull(tagName, "tagName");
    Objects.requireNonNull(tagTime, "tagTime");

    try {
      final var mapper = new ObjectMapper();
      final var blocks = mapper.createArrayNode();
      blocks.add(
        generateOKHeader(mapper, project, branch, commits, tagName, tagTime));
      final var root = mapper.createObjectNode();
      root.set("blocks", blocks);
      return mapper.writeValueAsString(root);
    } catch (final JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
