package com.riemannlab.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the mapping between pixels and the complex plane.
 *
 * <p>This is where every silent plotting error lives: an off-by-one at the
 * edges, sampling pixel corners instead of centres, a flipped vertical axis, or
 * non-square pixels quietly distorting the picture. None of those throw. They
 * produce a plausible-looking image that is wrong, which is the worst kind of
 * bug to have in a visualisation. Keeping the geometry in a record with no
 * dependency on {@code java.awt} is what lets it be tested by ordinary
 * assertions rather than by squinting.</p>
 */
class PlaneRegionTest {

    /**
     * A deliberately small region with exact arithmetic: four pixels wide by
     * two tall, each pixel exactly one unit square, so every expected value can
     * be worked out by hand.
     */
    private static final PlaneRegion FOUR_BY_TWO =
            new PlaneRegion(-2.0, 2.0, -1.0, 1.0, 4, 2);

    @Nested
    @DisplayName("Spans and scales")
    class Scales {

        @Test
        @DisplayName("Spans are the width and height of the rectangle")
        void spans() {
            assertEquals(4.0, FOUR_BY_TWO.realSpan(), 0.0);
            assertEquals(2.0, FOUR_BY_TWO.imaginarySpan(), 0.0);
        }

        @Test
        @DisplayName("Scale is span divided by pixel count")
        void scalePerPixel() {
            assertEquals(1.0, FOUR_BY_TWO.realPerPixel(), 0.0);
            assertEquals(1.0, FOUR_BY_TWO.imaginaryPerPixel(), 0.0);
        }

        @Test
        @DisplayName("Sample count is the pixel count, and is the evaluation count")
        void sampleCount() {
            assertEquals(8L, FOUR_BY_TWO.sampleCount());
            assertEquals(960_000L, new PlaneRegion(-1, 1, -1, 1, 800, 1200).sampleCount());
        }

        @Test
        @DisplayName("Square pixels give a distortion of exactly one")
        void squarePixelsAreUndistorted() {
            assertEquals(1.0, FOUR_BY_TWO.aspectDistortion(), 0.0);
        }

        @Test
        @DisplayName("A mismatched aspect ratio is reported rather than hidden")
        void stretchedPixelsAreReported() {
            PlaneRegion stretched = new PlaneRegion(-2.0, 2.0, -1.0, 1.0, 4, 4);

            assertEquals(2.0, stretched.aspectDistortion(), 1e-15,
                    "one unit of real per pixel against half a unit of imaginary");
        }
    }

    @Nested
    @DisplayName("The pixel to plane mapping")
    class Mapping {

        @Test
        @DisplayName("Pixels sample their centres, not their corners")
        void samplesPixelCentres() {
            Complex topLeft = FOUR_BY_TWO.pointAt(0, 0);

            assertEquals(-1.5, topLeft.real(), 1e-15,
                    "half a pixel in from the left edge at -2, not the edge itself");
            assertEquals(0.5, topLeft.imaginary(), 1e-15,
                    "half a pixel down from the top edge at +1");
        }

        @Test
        @DisplayName("The last pixel is half a step inside the far corner")
        void lastPixelIsInsideTheFarCorner() {
            Complex bottomRight = FOUR_BY_TWO.pointAt(3, 1);

            assertEquals(1.5, bottomRight.real(), 1e-15);
            assertEquals(-0.5, bottomRight.imaginary(), 1e-15);
        }

        @Test
        @DisplayName("Row zero is the top, which is the largest imaginary part")
        void verticalAxisIsFlipped() {
            double topRow = FOUR_BY_TWO.pointAt(0, 0).imaginary();
            double bottomRow = FOUR_BY_TWO.pointAt(0, 1).imaginary();

            assertTrue(topRow > bottomRow,
                    "screen y runs downward while the imaginary axis runs upward;"
                            + " top row was " + topRow + " and bottom row " + bottomRow);
        }

        @Test
        @DisplayName("Columns increase to the right, as the real axis does")
        void horizontalAxisIsNotFlipped() {
            assertTrue(FOUR_BY_TWO.pointAt(0, 0).real() < FOUR_BY_TWO.pointAt(3, 0).real());
        }

        @Test
        @DisplayName("Every sampled point lies strictly inside the rectangle")
        void everyPointIsInsideTheRectangle() {
            PlaneRegion region = new PlaneRegion(-3.0, 7.5, -2.25, 4.0, 13, 11);

            for (int y = 0; y < region.heightPixels(); y++) {
                for (int x = 0; x < region.widthPixels(); x++) {
                    Complex point = region.pointAt(x, y);

                    assertTrue(point.real() > region.minReal()
                                    && point.real() < region.maxReal(),
                            "real part escaped at (" + x + ", " + y + "): " + point);
                    assertTrue(point.imaginary() > region.minImaginary()
                                    && point.imaginary() < region.maxImaginary(),
                            "imaginary part escaped at (" + x + ", " + y + "): " + point);
                }
            }
        }

        @Test
        @DisplayName("A single pixel samples the centre of the region")
        void onePixelSamplesTheCentre() {
            Complex only = new PlaneRegion(2.0, 4.0, 10.0, 20.0, 1, 1).pointAt(0, 0);

            assertEquals(3.0, only.real(), 1e-15);
            assertEquals(15.0, only.imaginary(), 1e-15);
        }

        @Test
        @DisplayName("Pixels outside the grid are rejected")
        void outOfRangePixelsAreRejected() {
            assertThrows(IndexOutOfBoundsException.class, () -> FOUR_BY_TWO.pointAt(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> FOUR_BY_TWO.pointAt(4, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> FOUR_BY_TWO.pointAt(0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> FOUR_BY_TWO.pointAt(0, 2));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("A rectangle with no width or height is rejected")
        void degenerateRectanglesAreRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PlaneRegion(1.0, 1.0, -1.0, 1.0, 10, 10));
            assertThrows(IllegalArgumentException.class,
                    () -> new PlaneRegion(2.0, -2.0, -1.0, 1.0, 10, 10));
            assertThrows(IllegalArgumentException.class,
                    () -> new PlaneRegion(-1.0, 1.0, 5.0, 5.0, 10, 10));
            assertThrows(IllegalArgumentException.class,
                    () -> new PlaneRegion(-1.0, 1.0, 5.0, -5.0, 10, 10));
        }

        @Test
        @DisplayName("A grid with no pixels is rejected")
        void emptyGridsAreRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PlaneRegion(-1.0, 1.0, -1.0, 1.0, 0, 10));
            assertThrows(IllegalArgumentException.class,
                    () -> new PlaneRegion(-1.0, 1.0, -1.0, 1.0, 10, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> new PlaneRegion(-1.0, 1.0, -1.0, 1.0, -5, 10));
        }
    }
}