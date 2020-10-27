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

    for (final var key : parameters.keySet()) {
      System.out.printf("%s = %s\n", key, parameters.get(key));
    }

    return Status.SUCCESS;
  }

  private SortedMap<String, String> interpolate(
    final List<Properties> stack)
  {
    final var parameters = new TreeMap<String, String>();
    for (final var props : stack) {
      for (final var key : props.keySet()) {
        final var keyS = (String) key;
        final var valS = props.getProperty(keyS);
        parameters.put(keyS, valS);
      }
    }
    parameters.remove("inherits");
    return parameters;
  }

  private List<Properties> inheritanceStack(
    final Path fileRoot)
    throws IOException
  {
    final var stack = new LinkedList<Properties>();

    var fileCurrent = fileRoot;
    while (true) {
      this.logger().debug("process: {}", fileCurrent);
      final var fileName = fileCurrent.getFileName().toString();

      final var properties = new Properties();
      try (var stream = Files.newInputStream(fileCurrent)) {
        properties.load(stream);
      }
      stack.addFirst(properties);

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
