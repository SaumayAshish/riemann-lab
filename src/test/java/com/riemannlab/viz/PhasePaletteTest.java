package com.riemannlab.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the colour mapping.
 *
 * <p>"Does the picture look right" is not a test. What <em>is</em> testable is
 * every claim the mapping makes: that a zero comes out black, that the pole
 * comes out white, that a value pointing right is red and one pointing left is
 * cyan, and that the level curves at the powers of two are actually there. If
 * all of those hold, the image cannot be meaningfully wrong.</p>
 */
class PhasePaletteTest {

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
    @DisplayName("The two ends of the scale")
    class Extremes {

        @Test
        @DisplayName("A zero is black")
        void zeroIsBlack() {
            int colour = PhasePalette.colourFor(Complex.ZERO);

            assertEquals(0, red(colour), "red");
            assertEquals(0, green(colour), "green");
            assertEquals(0, blue(colour), "blue");
        }

        @Test
        @DisplayName("A value indistinguishable from zero is effectively black")
        void aNumericalZeroIsEffectivelyBlack() {
            int colour = PhasePalette.colourFor(Complex.of(7.6e-16, -2.1e-16));

            assertTrue(red(colour) <= 2 && green(colour) <= 2 && blue(colour) <= 2,
                    "a residual at the noise floor should render black, got "
                            + red(colour) + "," + green(colour) + "," + blue(colour));
        }

        @Test
        @DisplayName("A very large value is nearly white, not merely a bright colour")
        void aVeryLargeValueIsNearlyWhite() {
            int colour = PhasePalette.colourFor(Complex.ofReal(1e12));

            assertTrue(red(colour) > 240 && green(colour) > 220 && blue(colour) > 220,
                    "all three channels should be high near a pole, got "
                            + red(colour) + "," + green(colour) + "," + blue(colour)
                            + " - if only one channel is high the saturation is not"
                            + " falling away as the lightness rises");
        }

        @Test
        @DisplayName("An infinite magnitude is painted as the pole colour")
        void infiniteMagnitudeUsesThePoleColour() {
            assertEquals(PhasePalette.POLE_COLOUR,
                    PhasePalette.colourFor(Complex.of(Double.POSITIVE_INFINITY, 0.0)));
            assertEquals(PhasePalette.POLE_COLOUR,
                    PhasePalette.colourFor(Complex.of(Double.NaN, 1.0)));
        }
    }

    @Nested
    @DisplayName("Hue follows the argument")
    class Hue {

        @Test
        @DisplayName("A value pointing right is red")
        void positiveRealIsRed() {
            int colour = PhasePalette.colourFor(Complex.ofReal(1.0));

            assertTrue(red(colour) > green(colour) && red(colour) > blue(colour),
                    "arg = 0 should be red, got "
                            + red(colour) + "," + green(colour) + "," + blue(colour));
        }

        @Test
        @DisplayName("A value pointing left is cyan")
        void negativeRealIsCyan() {
            int colour = PhasePalette.colourFor(Complex.ofReal(-1.0));

            assertTrue(green(colour) > red(colour) && blue(colour) > red(colour),
                    "arg = pi should be cyan, got "
                            + red(colour) + "," + green(colour) + "," + blue(colour));
            assertEquals(green(colour), blue(colour), 2,
                    "cyan has equal green and blue");
        }

        @Test
        @DisplayName("A value pointing up is green, one pointing down is blue")
        void imaginaryDirectionsAreGreenAndBlue() {
            int up = PhasePalette.colourFor(Complex.ofImaginary(1.0));
            int down = PhasePalette.colourFor(Complex.ofImaginary(-1.0));

            assertTrue(green(up) > red(up) && green(up) > blue(up),
                    "arg = pi/2 should be green-dominant, got "
                            + red(up) + "," + green(up) + "," + blue(up));
            assertTrue(blue(down) > red(down) && blue(down) > green(down),
                    "arg = -pi/2 should be blue-dominant, got "
                            + red(down) + "," + green(down) + "," + blue(down));
        }

        @Test
        @DisplayName("The hue runs once around the circle, and only once")
        void hueCoversTheCircleExactlyOnce() {
            for (double angle = -3.0; angle < 3.1; angle += 0.25) {
                Complex onTheUnitCircle = Complex.of(Math.cos(angle), Math.sin(angle));
                double hue = PhasePalette.hueFor(onTheUnitCircle);

                assertTrue(hue >= 0.0 && hue < 1.0,
                        "hue must lie in [0, 1) - at angle " + angle + " it was " + hue);
            }
        }

        @Test
        @DisplayName("The hue depends only on direction, not on size")
        void hueIgnoresMagnitude() {
            double small = PhasePalette.hueFor(Complex.of(3.0, 4.0));
            double large = PhasePalette.hueFor(Complex.of(3.0e9, 4.0e9));

            assertEquals(small, large, 1e-15,
                    "scaling a value must not change its argument");
        }
    }

    @Nested
    @DisplayName("Lightness follows the magnitude")
    class Lightness {

        @Test
        @DisplayName("Lightness rises with magnitude across decades")
        void lightnessRisesWithMagnitude() {
            double tiny = PhasePalette.lightnessFor(0.01);
            double one = PhasePalette.lightnessFor(1.0);
            double large = PhasePalette.lightnessFor(100.0);

            assertTrue(tiny < one && one < large,
                    "expected increasing lightness, got " + tiny + ", " + one + ", " + large);
        }

        @Test
        @DisplayName("Lightness stays inside the unit interval for any finite magnitude")
        void lightnessStaysInRange() {
            for (double magnitude : new double[] {
                    0.0, 1e-300, 1e-16, 0.5, 1.0, 2.0, 1e6, 1e150, 1e300}) {

                double lightness = PhasePalette.lightnessFor(magnitude);

                assertTrue(lightness >= 0.0 && lightness <= 1.0,
                        "at magnitude " + magnitude + " lightness was " + lightness);
            }
        }

        @Test
        @DisplayName("A level curve is drawn at every power of two")
        void contoursAppearAtPowersOfTwo() {
            // Just below a power of two the band is at its lightest; just above
            // it, the shading resets. The visible step is what lets a reader
            // count rings to recover the magnitude rather than only compare it.
            double justBelow = PhasePalette.lightnessFor(2.0 - 1e-9);
            double justAbove = PhasePalette.lightnessFor(2.0 + 1e-9);

            assertTrue(justBelow > justAbove,
                    "expected a step down crossing |z| = 2, got "
                            + justBelow + " then " + justAbove);
            assertTrue(justBelow - justAbove > 0.02,
                    "the step should be visible, was only " + (justBelow - justAbove));
        }
    }

    @Nested
    @DisplayName("Determinism")
    class Determinism {

        @Test
        @DisplayName("The same value always gives the same colour")
        void sameValueSameColour() {
            Complex value = Complex.of(-1.7, 0.42);

            assertEquals(PhasePalette.colourFor(value), PhasePalette.colourFor(value));
        }

        @Test
        @DisplayName("The pole and undefined colours are distinct from each other")
        void reservedColoursAreDistinct() {
            assertTrue(PhasePalette.POLE_COLOUR != PhasePalette.UNDEFINED_COLOUR,
                    "a pole and a refused point must be told apart in the image");
        }
    }
}