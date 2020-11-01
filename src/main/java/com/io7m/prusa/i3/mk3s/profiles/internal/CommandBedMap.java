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
import com.io7m.jtensors.core.unparameterized.vectors.Vector3D;
import com.io7m.jtensors.core.unparameterized.vectors.Vectors3D;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

@Parameters(commandDescription = "Generate a heat map from bed levelling data")
public final class CommandBedMap extends CLPAbstractCommand
{
  private static final Pattern WHITESPACE =
    Pattern.compile("\\s+");

  @Parameter(
    names = "--file",
    required = true,
    description = "The file containing bed data from the G81 command")
  private Path file;

  @Parameter(
    names = "--output",
    required = true,
    description = "The output file containing a bed image")
  private Path output;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public CommandBedMap(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    try (Stream<String> lineStream = Files.lines(this.file)) {
      final var dataPoints =
        lineStream
          .filter(line -> !line.isBlank())
          .map(CommandBedMap::lineToVarianceArray)
          .collect(Collectors.toList());

      final var distinctWidths =
        dataPoints.stream()
          .map(row -> Integer.valueOf(row.size()))
          .distinct()
          .count();

      if (distinctWidths != 1L) {
        throw new IllegalArgumentException(
          "All rows of data must have the same length");
      }

      final var min =
        minMatrixValueOf(dataPoints);
      final var max =
        maxMatrixValueOf(dataPoints);

      final var logger = this.logger();
      logger.debug("minimum: {}", Double.valueOf(min));
      logger.debug("maximum: {}", Double.valueOf(max));

      final var height =
        dataPoints.size();
      final var width =
        dataPoints.stream()
          .mapToInt(List::size)
          .findFirst()
          .orElseThrow();

      final var image =
        new BufferedImage(width, height, TYPE_3BYTE_BGR);

      final var minColor =
        Vector3D.of(0.0, 0.0, 0.0);
      final var maxColor =
        Vector3D.of(0.0, 1.0, 1.0);

      {
        final var graphics =
          image.createGraphics();

        for (int y = 0; y < height; ++y) {
          final var row = dataPoints.get(y);
          for (int x = 0; x < width; ++x) {
            final var v =
              row.get(x).doubleValue();
            final var z =
              mapRange(v, min, max, 0.0, 1.0);
            final var map =
              Vectors3D.interpolateLinear(minColor, maxColor, z);

            logger.debug("[{} {}] {} -> {}",
                         Integer.valueOf(x), Integer.valueOf(y),
                         Double.valueOf(v), Double.valueOf(z));

            graphics.setPaint(new Color(
              (float) map.x(),
              (float) map.y(),
              (float) map.z()));
            graphics.fillRect(x, y, 1, 1);
          }
        }

        graphics.dispose();
      }

      final BufferedImage canvas =
        new BufferedImage(800, 608, image.getType());

      {
        final var graphics = canvas.createGraphics();
        graphics.setPaint(Color.WHITE);
        graphics.fillRect(0, 0, 800, 608);
        graphics.setRenderingHint(
          KEY_INTERPOLATION,
          VALUE_INTERPOLATION_BILINEAR);

        graphics.drawImage(
          image,
          32,
          32,
          512 + 32,
          512 + 32,
          0,
          0,
          image.getWidth(),
          image.getHeight(),
          null
        );

        graphics.setPaint(Color.BLACK);
        graphics.drawRect(32, 32, 512, 512);

        final var gradient =
          new GradientPaint(
            0.0f,
            0.0f,
            new Color(
              (float) minColor.x(),
              (float) minColor.y(),
              (float) minColor.z()
            ),
            0.0f,
            512.0f,
            new Color(
              (float) maxColor.x(),
              (float) maxColor.y(),
              (float) maxColor.z()
            )
          );

        graphics.setPaint(gradient);
        graphics.fillRect(704, 32, 64, 512);
        graphics.setPaint(Color.BLACK);
        graphics.drawRect(704, 32, 64, 512);
        graphics.drawString(String.format("%.6fmm", Double.valueOf(min)), 704 - 96, 32 + 16);
        graphics.drawString(String.format("%.6fmm", Double.valueOf(max)), 704 - 96, 608 - 64);
        graphics.dispose();
      }

      try (var stream = Files.newOutputStream(
        this.output,
        WRITE,
        TRUNCATE_EXISTING,
        CREATE)) {
        ImageIO.write(canvas, "PNG", stream);
      }
    }

    return Status.SUCCESS;
  }

  private static double mapRange(
    final double x,
    final double srcLow,
    final double srcHigh,
    final double destLow,
    final double destHigh)
  {
    final double ratio = (x - srcLow) / (srcHigh - srcLow);
    return ratio * (destHigh - destLow) + destLow;
  }

  private static double minValueOf(
    final List<Double> xs)
  {
    return xs.stream()
      .mapToDouble(Double::doubleValue)
      .min()
      .orElse(0.0);
  }

  private static double minMatrixValueOf(
    final List<List<Double>> xs)
  {
    return xs.stream()
      .mapToDouble(CommandBedMap::minValueOf)
      .min()
      .orElse(0.0);
  }

  private static double maxMatrixValueOf(
    final List<List<Double>> xs)
  {
    return xs.stream()
      .mapToDouble(CommandBedMap::maxValueOf)
      .max()
      .orElse(0.0);
  }

  private static double maxValueOf(
    final List<Double> xs)
  {
    return xs.stream()
      .mapToDouble(Double::doubleValue)
      .max()
      .orElse(0.0);
  }

  private static List<Double> lineToVarianceArray(
    final String line)
  {
    return Arrays.stream(WHITESPACE.split(line))
      .mapToDouble(Double::parseDouble)
      .boxed()
      .collect(Collectors.toList());
  }

  @Override
  public String name()
  {
    return "bed-map";
  }
}
