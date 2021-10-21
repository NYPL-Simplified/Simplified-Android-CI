package org.librarysimplified.ci.check_commits_since;

import com.beust.jcommander.JCommander;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.librarysimplified.ci.ExitException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class CheckCommitsSince
{
  private static final Logger LOG = Logger.getLogger("CheckCommitsSince");

  private static final Map<String, CheckCommitsSinceStatusFormatterType> FORMATTERS =
    Stream.of(
      new CheckCommitsSinceStatusPlainFormatter(),
      new CheckCommitsSinceStatusSlackFormatter()
    ).collect(Collectors.toMap(
      CheckCommitsSinceStatusFormatterType::name,
      Function.identity())
    );

  private CheckCommitsSince()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var parameters = new CheckCommitsSinceParameters();

    try {
      JCommander.newBuilder()
        .addObject(parameters)
        .build()
        .parse(args);
    } catch (final Exception e) {
      LOG.severe("Error parsing arguments: " + e.getMessage());
      throw new ExitException(1);
    }

    final var formatter = FORMATTERS.get(parameters.formatterName);
    if (formatter == null) {
      LOG.severe("No formatter exists with the name '" + parameters.formatterName + "'");
      LOG.severe("Existing formatters include: " + FORMATTERS.keySet());
      throw new ExitException(1);
    }

    try (var git = Git.open(parameters.gitRepository.toFile())) {
      processBranch(parameters, git, formatter);
    }
  }

  /**
   * Look up the first commit associated with the given tag.
   *
   * @param git     The git repository
   * @param tagName The tag name
   * @param tagRef  The tag reference
   *
   * @return The first commit associated with the tag
   *
   * @throws Exception On errors
   */

  private static RevCommit commitForTag(
    final Git git,
    final String tagName,
    final Ref tagRef)
    throws Exception
  {
    final var log =
      git.log();
    final var repository =
      git.getRepository();
    final var database =
      repository.getRefDatabase();

    final var peeledRef = database.peel(tagRef);
    if (peeledRef.getPeeledObjectId() != null) {
      log.add(peeledRef.getPeeledObjectId());
    } else {
      log.add(tagRef.getObjectId());
    }

    return StreamSupport.stream(log.call().spliterator(), false)
      .limit(1L)
      .findFirst()
      .orElseThrow(() -> new IOException("No commit for tag " + tagName));
  }

  /**
   * Find the latest tag in the repository.
   *
   * @param git The git repository
   *
   * @return The latest tag, if there is one
   *
   * @throws Exception On errors
   */

  private static Optional<TagWithCommit> findLatestTag(
    final Git git)
    throws Exception
  {
    final var tagList =
      git.tagList()
        .call();

    final var tagsWithCommits =
      new ArrayList<TagWithCommit>(tagList.size());

    for (final var tag : tagList) {
      final var commit = commitForTag(git, tag.getName(), tag);
      tagsWithCommits.add(new TagWithCommit(tag, tag.getName(), commit));
    }

    tagsWithCommits.sort(Comparator.reverseOrder());
    if (tagsWithCommits.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(tagsWithCommits.get(0));
  }

  private static void processBranch(
    final CheckCommitsSinceParameters parameters,
    final Git git,
    final CheckCommitsSinceStatusFormatterType formatter)
    throws Exception
  {
    /*
     * Find the latest tag in the repository. All tags are assumed to
     * be releases.
     */

    final var latestTagOpt =
      findLatestTag(git);

    if (latestTagOpt.isEmpty()) {
      System.out.println("No tags exist in the given repository.");
      return;
    }

    /*
     * Find the number of commits that have been made on the given branch
     * since the latest tag.
     */

    final var latestTag =
      latestTagOpt.get();
    final var commitsSince =
      findCommitsOnBranchSince(git, parameters.branchName, latestTag);

    /*
     * If more time has passed since the last release than is allowed, and
     * if there has actually been significant work done on the given branch,
     * then fail the check and demand a release.
     */

    final var timeNow =
      Instant.now(Clock.systemUTC());
    final var timeTag =
      Instant.ofEpochSecond(
        Integer.toUnsignedLong(latestTag.taggedCommit.getCommitTime()));
    final var timeExpectedRelease =
      timeTag.plus(Duration.ofDays(parameters.releaseDays));

    if (timeNow.isAfter(timeExpectedRelease)) {
      if (commitsSince.size() >= parameters.commitCount) {
        System.out.println(
          formatter.failed(
            parameters.projectName,
            parameters.branchName,
            commitsSince.size(),
            latestTag.tagName,
            timeTag
          )
        );
        throw new ExitException(1);
      }
    }
  }

  private static List<RevCommit> findCommitsOnBranchSince(
    final Git git,
    final String branchName,
    final TagWithCommit latestTag)
    throws Exception
  {
    /*
     * Find the commit that points to the HEAD of the branch.
     */

    final var repository =
      git.getRepository();
    final var head =
      repository.exactRef(String.format("refs/heads/%s", branchName));

    /*
     * Determine the time of the tagged commit. We'll only return
     * commits newer than this.
     */

    final var timeTag =
      Integer.toUnsignedLong(latestTag.taggedCommit.getCommitTime());

    /*
     * Start walking through the list of commits, backwards through time
     * (newest commits will be returned first).
     */

    try (var walk = new RevWalk(repository)) {
      final var startCommit = walk.parseCommit(head.getObjectId());
      walk.markStart(startCommit);

      final var commits = new LinkedList<RevCommit>();
      for (final var revCommit : walk) {
        final var timeThis =
          Integer.toUnsignedLong(revCommit.getCommitTime());

        if (Long.compareUnsigned(timeThis, timeTag) > 0) {
          commits.addFirst(revCommit);
        } else {
          break;
        }
      }
      return commits;
    }
  }

  /**
   * A tag with an associated commit.
   */

  private static final class TagWithCommit implements Comparable<TagWithCommit>
  {
    private final Ref tag;
    private final String tagName;
    private final RevCommit taggedCommit;

    private TagWithCommit(
      final Ref inTag,
      final String inTagName,
      final RevCommit inTaggedCommit)
    {
      this.tag =
        Objects.requireNonNull(inTag, "tag");
      this.tagName =
        Objects.requireNonNull(inTagName, "tagName");
      this.taggedCommit =
        Objects.requireNonNull(inTaggedCommit, "taggedCommit");
    }

    @Override
    public int compareTo(
      final TagWithCommit other)
    {
      final var timeThis =
        Integer.toUnsignedLong(this.taggedCommit.getCommitTime());
      final var timeOther =
        Integer.toUnsignedLong(other.taggedCommit.getCommitTime());

      return Long.compareUnsigned(timeThis, timeOther);
    }
  }
}
