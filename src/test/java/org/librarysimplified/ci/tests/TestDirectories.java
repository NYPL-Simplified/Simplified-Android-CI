
package org.librarysimplified.ci.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

public final class TestDirectories
{
  private TestDirectories()
  {

  }

  public static Path createBaseDirectory()
    throws IOException
  {
    final var property = System.getProperty("java.io.tmpdir");
    if (property.isBlank() || property.isEmpty()) {
      throw new IllegalStateException();
    }

    final var path = Path.of(property).resolve("ci");
    Files.createDirectories(path);
    return path;
  }

  public static Path createTempDirectory()
    throws IOException
  {
    final var path = createBaseDirectory();
    final var temp = path.resolve(UUID.randomUUID().toString());
    Files.createDirectories(temp);
    return temp;
  }

  public static void deleteDirectory(
    final Path directory)
    throws IOException
  {
    try (var walk = Files.walk(directory)) {
      walk.sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    }
  }

  public static Path resourceOf(
    final Class<?> clazz,
    final Path output,
    final String name)
    throws IOException
  {
    final var internal = String.format("/org/librarysimplified/ci/tests/%s", name);
    final var url = clazz.getResource(internal);
    if (url == null) {
      throw new NoSuchFileException(internal);
    }

    final var target = output.resolve(name);
    try (var stream = url.openStream()) {
      Files.copy(stream, target);
    }
    return target;
  }

  public static InputStream resourceStreamOf(
    final Class<?> clazz,
    final Path output,
    final String name)
    throws IOException
  {
    return Files.newInputStream(resourceOf(clazz, output, name));
  }

  public static byte[] resourceBytesOf(
    final Class<?> clazz,
    final Path output,
    final String name)
    throws IOException
  {
    return resourceStreamOf(clazz, output, name).readAllBytes();
  }
}
