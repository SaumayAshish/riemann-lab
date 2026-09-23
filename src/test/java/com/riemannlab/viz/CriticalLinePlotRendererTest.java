package com.riemannlab.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.analysis.CriticalLineSample;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the plot's geometry, not its aesthetics. Anti-aliasing is
 * disabled in the renderer specifically so these pixel checks are
 * deterministic rather than dependent on how a particular JVM blends edges.
 */
class CriticalLinePlotRendererTest {

    private static final int WIDTH = 240;
    private static final int HEIGHT = 160;

    private final CriticalLinePlotRenderer renderer = new CriticalLinePlotRenderer();

    private static List<CriticalLineSample> flatSamplesWithOnePeakAt(
            double peakHeight, double peakMagnitude) {

        return List.of(
                new CriticalLineSample(0.0, 0.05),
                new CriticalLineSample(peakHeight, peakMagnitude),
                new CriticalLineSample(10.0, 0.05));
    }

    private static boolean columnContains(BufferedImage image, int x, int rgb) {
        for (int y = 0; y < image.getHeight(); y++) {
            if (image.getRGB(x, y) == rgb) {
                return true;
            }
        }
        return false;
    }

    private static int topmostRowOf(BufferedImage image, int x, int rgb) {
        for (int y = 0; y < image.getHeight(); y++) {
            if (image.getRGB(x, y) == rgb) {
                return y;
            }
        }
        return -1;
    }

    @Nested
    @DisplayName("The image as a whole")
    class Image {

        @Test
        @DisplayName("The image has exactly the requested dimensions")
        void dimensionsMatchTheRequest() {
            BufferedImage image = renderer.render(
                    flatSamplesWithOnePeakAt(5.0, 1.0), List.of(), WIDTH, HEIGHT);

            assertEquals(WIDTH, image.getWidth());
            assertEquals(HEIGHT, image.getHeight());
        }

        @Test
        @DisplayName("A corner well inside the margin is left as background")
        void backgroundIsUntouchedInTheCorner() {
            BufferedImage image = renderer.render(
                    flatSamplesWithOnePeakAt(5.0, 1.0), List.of(), WIDTH, HEIGHT);

            assertEquals(0xFFFFFFFF, image.getRGB(WIDTH - 3, 3),
                    "the top-right corner should be untouched white background");
        }

        @Test
        @DisplayName("Rendering is deterministic")
        void isDeterministic() {
            List<CriticalLineSample> samples = flatSamplesWithOnePeakAt(5.0, 1.0);

            BufferedImage first = renderer.render(samples, List.of(14.13), WIDTH, HEIGHT);
            BufferedImage second = renderer.render(samples, List.of(14.13), WIDTH, HEIGHT);

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    assertEquals(first.getRGB(x, y), second.getRGB(x, y),
                            "pixel (" + x + "," + y + ") differed between identical renders");
                }
            }
        }
    }

    @Nested
    @DisplayName("Axes")
    class Axes {

        @Test
        @DisplayName("The horizontal axis is drawn along the baseline")
        void horizontalAxisIsDrawn() {
            BufferedImage image = renderer.render(
                    flatSamplesWithOnePeakAt(5.0, 1.0), List.of(), WIDTH, HEIGHT);

            int baseline = HEIGHT - CriticalLinePlotRenderer.MARGIN;
            assertEquals(0xFF000000, image.getRGB(WIDTH / 2, baseline),
                    "expected the axis colour along the baseline row");
        }

        @Test
        @DisplayName("The vertical axis is drawn along the left margin")
        void verticalAxisIsDrawn() {
            BufferedImage image = renderer.render(
                    flatSamplesWithOnePeakAt(5.0, 1.0), List.of(), WIDTH, HEIGHT);

            int margin = CriticalLinePlotRenderer.MARGIN;
            assertEquals(0xFF000000, image.getRGB(margin, HEIGHT / 2),
                    "expected the axis colour along the left margin column");
        }
    }

    @Nested
    @DisplayName("The curve")
    class Curve {

        @Test
        @DisplayName("A taller magnitude renders higher up than a shorter one")
        void tallerMagnitudeIsHigher() {
            List<CriticalLineSample> samples = List.of(
                    new CriticalLineSample(0.0, 0.1),
                    new CriticalLineSample(2.0, 1.0),
                    new CriticalLineSample(4.0, 0.1),
                    new CriticalLineSample(6.0, 3.0),
                    new CriticalLineSample(8.0, 0.1),
                    new CriticalLineSample(10.0, 0.1));

            BufferedImage image = renderer.render(samples, List.of(), WIDTH, HEIGHT);

            PlotLayout layout = new PlotLayout(0.0, 10.0, 0.0, 3.0 * 1.1, WIDTH, HEIGHT,
                    CriticalLinePlotRenderer.MARGIN);
            int columnOfSmallPeak = (int) Math.round(layout.xPixel(2.0));
            int columnOfTallPeak = (int) Math.round(layout.xPixel(6.0));

            int rowOfSmallPeak = topmostRowOf(image, columnOfSmallPeak, 0xFF1A5FB0);
            int rowOfTallPeak = topmostRowOf(image, columnOfTallPeak, 0xFF1A5FB0);

            assertTrue(rowOfSmallPeak >= 0 && rowOfTallPeak >= 0,
                    "expected curve-coloured pixels in both peak columns");
            assertTrue(rowOfTallPeak < rowOfSmallPeak,
                    "the taller peak (magnitude 3.0) should render above (smaller row than) "
                            + "the shorter one (magnitude 1.0); tall=" + rowOfTallPeak
                            + " short=" + rowOfSmallPeak);
        }
    }

    @Nested
    @DisplayName("Zero markers")
    class ZeroMarkers {

        @Test
        @DisplayName("A zero height inside the sampled range is marked")
        void markerIsDrawnInsideRange() {
            List<CriticalLineSample> samples = flatSamplesWithOnePeakAt(5.0, 1.0);
            BufferedImage image = renderer.render(samples, List.of(5.0), WIDTH, HEIGHT);

            PlotLayout layout = new PlotLayout(0.0, 10.0, 0.0, 1.1, WIDTH, HEIGHT,
                    CriticalLinePlotRenderer.MARGIN);
            int column = (int) Math.round(layout.xPixel(5.0));

            assertTrue(columnContains(image, column, 0xFFD01C1C),
                    "expected the zero-marker colour in the column at height 5.0");
        }

        @Test
        @DisplayName("No markers are drawn when no zero heights are given")
        void noMarkersWhenListIsEmpty() {
            BufferedImage image = renderer.render(
                    flatSamplesWithOnePeakAt(5.0, 1.0), List.of(), WIDTH, HEIGHT);

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    assertFalse(image.getRGB(x, y) == 0xFFD01C1C,
                            "no zero heights were supplied, so no marker colour should appear");
                }
            }
        }

        @Test
        @DisplayName("A zero height outside the sampled range is skipped, not an error")
        void outOfRangeHeightIsSkipped() {
            BufferedImage image = renderer.render(
                    flatSamplesWithOnePeakAt(5.0, 1.0), List.of(500.0), WIDTH, HEIGHT);

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    assertFalse(image.getRGB(x, y) == 0xFFD01C1C,
                            "a height outside [0, 10] must not be drawn anywhere");
                }
            }
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Null samples or zero heights are rejected")
        void rejectsNulls() {
            assertThrows(NullPointerException.class,
                    () -> renderer.render(null, List.of(), WIDTH, HEIGHT));
            assertThrows(NullPointerException.class,
                    () -> renderer.render(flatSamplesWithOnePeakAt(5.0, 1.0), null, WIDTH, HEIGHT));
        }

        @Test
        @DisplayName("Fewer than two samples is rejected")
        void rejectsTooFewSamples() {
            assertThrows(IllegalArgumentException.class,
                    () -> renderer.render(
                            List.of(new CriticalLineSample(0.0, 1.0)), List.of(), WIDTH, HEIGHT));
        }

        @Test
        @DisplayName("A canvas too small for the margin is rejected")
        void rejectsUndersizedCanvas() {
            assertThrows(IllegalArgumentException.class,
                    () -> renderer.render(
                            flatSamplesWithOnePeakAt(5.0, 1.0), List.of(), 50, 50));
        }
    }
}