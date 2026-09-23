package com.riemannlab.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.zeta.ContinuedZetaEvaluator;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the rendered image says what the mathematics says.
 *
 * <p>The technique throughout is to render a one-pixel region centred on a
 * point whose value is already known from earlier phases, and assert the
 * colour. That turns "is the picture right" into an ordinary equality check
 * against facts the project has already established: the first zero is at
 * {@code 0.5 + 14.1347i}, the pole is at {@code s = 1}, and the evaluator
 * refuses points far to the left.</p>
 */
class DomainColourRendererTest {

    private static final double FIRST_ZERO = 14.134725141734693;

    private final DomainColourRenderer renderer =
            new DomainColourRenderer(new ContinuedZetaEvaluator());

    /** A region one pixel across, centred on the given point. */
    private static PlaneRegion singlePixelAt(double real, double imaginary, double radius) {
        return new PlaneRegion(
                real - radius, real + radius,
                imaginary - radius, imaginary + radius,
                1, 1);
    }

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }

    @Nested
    @DisplayName("Known points render to known colours")
    class KnownPoints {

        @Test
        @DisplayName("The first non-trivial zero renders black")
        void theFirstZeroIsBlack() {
            int pixel = renderer
                    .render(singlePixelAt(0.5, FIRST_ZERO, 1e-6))
                    .getRGB(0, 0);

            assertTrue(red(pixel) <= 5 && green(pixel) <= 5 && blue(pixel) <= 5,
                    "a zero must render black, got "
                            + red(pixel) + "," + green(pixel) + "," + blue(pixel));
        }

        @Test
        @DisplayName("A trivial zero renders black too")
        void aTrivialZeroIsBlack() {
            int pixel = renderer
                    .render(singlePixelAt(-4.0, 0.0, 1e-9))
                    .getRGB(0, 0);

            assertTrue(red(pixel) <= 5 && green(pixel) <= 5 && blue(pixel) <= 5,
                    "the trivial zero at s = -4 must render black, got "
                            + red(pixel) + "," + green(pixel) + "," + blue(pixel));
        }

        @Test
        @DisplayName("The pole at s = 1 is painted with the reserved pole colour")
        void thePoleUsesTheReservedColour() {
            int pixel = renderer
                    .render(singlePixelAt(1.0, 0.0, 1e-12))
                    .getRGB(0, 0);

            assertEquals(PhasePalette.POLE_COLOUR, pixel,
                    "the evaluator refuses s = 1, and a refusal must be painted"
                            + " rather than propagated");
        }

        @Test
        @DisplayName("Just outside the pole the picture is nearly white")
        void besideThePoleIsNearlyWhite() {
            int pixel = renderer
                    .render(singlePixelAt(1.001, 0.0, 1e-9))
                    .getRGB(0, 0);

            assertTrue(red(pixel) > 150 && green(pixel) > 150 && blue(pixel) > 150,
                    "|zeta(1.001)| is about 1000, which should be near white, got "
                            + red(pixel) + "," + green(pixel) + "," + blue(pixel));
        }

        @Test
        @DisplayName("A point the evaluator refuses is painted with the undefined colour")
        void refusedPointsAreVisible() {
            int pixel = renderer
                    .render(singlePixelAt(-500.0, 0.0, 1e-6))
                    .getRGB(0, 0);

            assertEquals(PhasePalette.UNDEFINED_COLOUR, pixel,
                    "the limits of the evaluator should be visible in the output,"
                            + " not hidden behind an exception or a plausible colour");
        }
    }

    @Nested
    @DisplayName("The image as a whole")
    class Image {

        @Test
        @DisplayName("The image has exactly the requested dimensions")
        void dimensionsMatchTheRegion() {
            BufferedImage image =
                    renderer.render(new PlaneRegion(-2.0, 2.0, -2.0, 2.0, 17, 11));

            assertEquals(17, image.getWidth());
            assertEquals(11, image.getHeight());
        }

        @Test
        @DisplayName("Every pixel is painted, and the picture has real variety")
        void everyPixelIsPaintedAndVaried() {
            BufferedImage image =
                    renderer.render(new PlaneRegion(-3.0, 3.0, -3.0, 3.0, 24, 24));

            Set<Integer> colours = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    colours.add(image.getRGB(x, y));
                }
            }

            assertTrue(colours.size() > 50,
                    "a region spanning the pole and the critical strip should be"
                            + " richly coloured, but only " + colours.size()
                            + " distinct colours appeared - a mostly uniform image"
                            + " usually means the lightness ramp has collapsed");
        }

        @Test
        @DisplayName("Rendering is deterministic despite running in parallel")
        void parallelRenderingIsDeterministic() {
            PlaneRegion region = new PlaneRegion(-2.0, 2.0, 10.0, 16.0, 20, 30);

            BufferedImage first = renderer.render(region);
            BufferedImage second = renderer.render(region);

            for (int y = 0; y < region.heightPixels(); y++) {
                for (int x = 0; x < region.widthPixels(); x++) {
                    assertEquals(first.getRGB(x, y), second.getRGB(x, y),
                            "pixel (" + x + ", " + y + ") differed between runs;"
                                    + " the evaluators are documented thread-safe and"
                                    + " the row loop is parallel, so any difference"
                                    + " here is a race");
                }
            }
        }

        @Test
        @DisplayName("The top of the image is the larger imaginary part")
        void verticalOrientationSurvivesRendering() {
            // A one-pixel-wide strip up the critical line, positioned so the
            // first zero sits near the TOP of the imaginary range. If the
            // vertical axis is mapped correctly the darkest row is near index
            // 0; if the flip were missing it would be near the last row. This
            // is the one plotting error that produces a perfectly plausible
            // upside-down picture, so it is worth a test of its own.
            PlaneRegion region =
                    new PlaneRegion(0.45, 0.55, FIRST_ZERO - 0.9, FIRST_ZERO + 0.1, 1, 10);
            BufferedImage image = renderer.render(region);

            int darkestRow = 0;
            int darkestBrightness = Integer.MAX_VALUE;

            for (int y = 0; y < region.heightPixels(); y++) {
                int brightness = brightnessOf(image.getRGB(0, y));
                if (brightness < darkestBrightness) {
                    darkestBrightness = brightness;
                    darkestRow = y;
                }
            }

            assertTrue(darkestRow <= 1,
                    "the zero has the largest imaginary part in this strip, so it must"
                            + " appear near the top of the image; the darkest row was "
                            + darkestRow + " of " + region.heightPixels());
        }

        /** The largest channel, which is the HSB brightness scaled to a byte. */
        private int brightnessOf(int argb) {
            return Math.max(red(argb), Math.max(green(argb), blue(argb)));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Null arguments are rejected")
        void rejectsNulls() {
            assertThrows(NullPointerException.class, () -> new DomainColourRenderer(null));
            assertThrows(NullPointerException.class, () -> renderer.render(null));
        }

        @Test
        @DisplayName("A one-pixel image is still a valid image")
        void onePixelIsAllowed() {
            BufferedImage image = renderer.render(singlePixelAt(2.0, 0.0, 0.1));

            assertEquals(1, image.getWidth());
            assertEquals(1, image.getHeight());
            assertNotEquals(0, image.getRGB(0, 0),
                    "zeta(2) is about 1.64, which is not black");
        }
    }
}
