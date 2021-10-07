package org.librarysimplified.ci;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import static java.net.http.HttpResponse.BodyHandlers.ofInputStream;

/**
 * A library that can be checked against a repository.
 */

public final class CheckVersionLibrary implements Comparable<CheckVersionLibrary>
{
  private final String group;
  private final String artifact;
  private final DefaultArtifactVersion version;
  private final Set<String> checkRepositories;
  private final boolean ignore;

  CheckVersionLibrary(
    final String inGroup,
    final String inArtifact,
    final DefaultArtifactVersion inVersion,
    final Set<String> inCheckRepositories,
    final boolean inIgnore)
  {
    this.group =
      Objects.requireNonNull(inGroup, "group");
    this.artifact =
      Objects.requireNonNull(inArtifact, "artifact");
    this.version =
      Objects.requireNonNull(inVersion, "version");
    this.checkRepositories =
      Objects.requireNonNull(inCheckRepositories, "checkRepositories");
    this.ignore = inIgnore;
  }

  public boolean isIgnored()
  {
    return this.ignore;
  }

  public String group()
  {
    return this.group;
  }

  public String artifact()
  {
    return this.artifact;
  }

  public DefaultArtifactVersion version()
  {
    return this.version;
  }

  CheckVersionLibraryStatusType check(
    final HttpClient httpClient)
  {
    if (this.ignore) {
      return new CheckVersionLibraryStatusIgnored(this);
    }

    final var groupSlashes =
      this.group.replace('.', '/');

    for (final var baseServer : this.checkRepositories) {
      final var serverWithoutSlash =
        baseServer.replace("/+$", "");
      final var targetURI =
        URI.create(
          new StringBuilder(64)
            .append(serverWithoutSlash)
            .append(groupSlashes)
            .append("/")
            .append(this.artifact)
            .append("/maven-metadata.xml")
            .toString()
        );

      final var request =
        HttpRequest.newBuilder(targetURI)
          .GET()
          .build();

      try {
        final var response =
          httpClient.send(request, ofInputStream());

        final var statusCode = response.statusCode();
        if (statusCode == 404) {
          continue;
        }

        if (statusCode >= 500) {
          return new CheckVersionLibraryStatusError(
            this, targetURI + ": " + statusCode);
        }

        try (var stream = response.body()) {
          return this.findLatestVersion(stream);
        }
      } catch (final IOException | InterruptedException e) {
        var message = e.getMessage();
        if (message == null) {
          message = e.getClass().getSimpleName();
        }
        return new CheckVersionLibraryStatusError(this, message);
      }
    }

    return new CheckVersionLibraryStatusUnavailable(this);
  }

  private CheckVersionLibraryStatusType findLatestVersion(
    final InputStream stream)
    throws IOException
  {
    try {
      final XPath xpath =
        XPathFactory.newInstance()
          .newXPath();
      final XPathExpression expression =
        xpath.compile("/metadata/versioning/latest");

      final var documentBuilders =
        DocumentBuilderFactory.newDefaultInstance();
      final var documentBuilder =
        documentBuilders.newDocumentBuilder();
      final var document =
        documentBuilder.parse(stream);

      final var availableVersionText =
        expression.evaluate(document);

      if (availableVersionText == null || availableVersionText.isBlank()) {
        throw new IOException(
          String.format(
            "Received unparseable version number '%s' from server",
            availableVersionText)
        );
      }

      final var availableVersion =
        new DefaultArtifactVersion(availableVersionText);

      if (availableVersion.compareTo(this.version) > 0) {
        return new CheckVersionLibraryStatusOutOfDate(this, availableVersion);
      }

      return new CheckVersionLibraryStatusUpToDate(this);
    } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
      throw new IOException(e);
    }
  }

  @Override
  public int compareTo(
    final CheckVersionLibrary other)
  {
    return Comparator.comparing(CheckVersionLibrary::group)
      .thenComparing(CheckVersionLibrary::artifact)
      .thenComparing(CheckVersionLibrary::version)
      .compare(this, other);
  }
}
