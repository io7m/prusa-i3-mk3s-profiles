/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.prusa.i3.mk3s.profiles.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

@Parameters(commandDescription = "Interpolate profiles using inheritance")
public final class CommandInterpolate extends CLPAbstractCommand
{
  private static final Pattern INI_SUFFIX =
    Pattern.compile("\\.ini$");

  @Parameter(
    names = "--file",
    required = true,
    description = "The file to interpolate")
  private Path file;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public CommandInterpolate(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    final var stack =
      this.inheritanceStack(this.file);
    final var parameters =
      this.interpolate(stack);

    try (var stream = CommandInterpolate.class.getResourceAsStream(
      "/com/io7m/prusa/i3/mk3s/profiles/header.txt")) {
      stream.transferTo(System.out);
    }

    System.out.printf("#  Source: https://www.github.com/io7m/prusa-i3-mk3s-profiles\n");
    System.out.printf("#\n");
    for (int index = stack.size() - 1; index >= 0; --index) {
      final var superclass = stack.get(index);
      if (index == stack.size() - 1) {
        System.out.printf("#  Profile: %s\n", superclass.file.getFileName());
      } else {
        System.out.printf("#         ↳ %s\n", superclass.file.getFileName());
      }
    }
    System.out.printf("#\n");
    System.out.println();

    for (final var key : parameters.keySet()) {
      final var value = parameters.get(key);
      if (!value.isBlank()) {
        System.out.printf("%s = %s\n", key, value);
      } else {
        System.out.printf("%s =\n", key);
      }
    }

    return Status.SUCCESS;
  }

  private SortedMap<String, String> interpolate(
    final List<Superclass> stack)
  {
    final var parameters = new TreeMap<String, String>();
    for (final var superclass : stack) {
      final var props = superclass.properties;
      for (final var key : props.keySet()) {
        final var keyS = (String) key;
        final var valS = props.getProperty(keyS).replace("\n", "\\n");
        parameters.put(keyS, valS);
      }
    }
    parameters.remove("inherits");
    return parameters;
  }

  private static final class Superclass
  {
    private Properties properties;
    private Path file;

    Superclass(
      final Properties inProperties,
      final Path inFile)
    {
      this.properties =
        Objects.requireNonNull(inProperties, "properties");
      this.file =
        Objects.requireNonNull(inFile, "file");
    }
  }

  private List<Superclass> inheritanceStack(
    final Path fileRoot)
    throws IOException
  {
    final var stack = new LinkedList<Superclass>();

    var fileCurrent = fileRoot;
    while (true) {
      this.logger().debug("process: {}", fileCurrent);
      final var fileName = fileCurrent.getFileName().toString();

      final var properties = new Properties();
      try (var stream = Files.newInputStream(fileCurrent)) {
        properties.load(stream);
      }
      stack.addFirst(new Superclass(properties, fileCurrent));

      final var inherits =
        Optional.ofNullable(properties.getProperty("inherits"))
          .map(String::trim)
          .orElse(fileName);

      final var fileNameNoIni =
        INI_SUFFIX.matcher(fileName).replaceAll("");

      if (inherits.equals(fileNameNoIni)) {
        return stack;
      }

      fileCurrent =
        fileCurrent.resolveSibling(String.format("%s.ini", inherits));
    }
  }

  @Override
  public String name()
  {
    return "interpolate";
  }
}
