package com.riemannlab.core.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.complex.ComplexMath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the complex gamma function against values known in closed form, and
 * against the identities that characterise it.
 *
 * <p>Comparisons are <strong>relative</strong>, not absolute. Gamma spans forty
 * orders of magnitude over the region this project cares about - it is 362880
 * at s = 10 and about 2e-17 at s = 0.5 + 25i - so a fixed absolute tolerance
 * would be vacuous at one end and unsatisfiable at the other.</p>
 *
 * <p>The identity tests matter more than the table of known values. A lookup
 * table only shows the implementation agrees with itself at the points someone
 * thought to check. The recurrence and the reflection formula must hold at
 * <em>every</em> point, so they test the function rather than a sample of
 * it.</p>
 */
class GammaFunctionTest {

    private static final double RELATIVE_TOLERANCE = 1e-12;

    private static final double SQRT_PI = 1.7724538509055159;

    /**
     * Sample points chosen to exercise both branches: some with
     * {@code Re(z) >= 1/2}, which use the series directly, and some to the
     * left of it, which are reached through the reflection formula. None of
     * them, and none of their reflections {@code 1 - z}, is a non-positive
     * integer, because gamma has poles there.
     */
    private static final Complex[] SAMPLES = {
            Complex.of(0.3, 0.4),
            Complex.of(2.5, -1.25),
            Complex.of(-3.7, 2.1),
            Complex.of(0.5, 14.134725141734693),
            Complex.of(6.75, 0.0),
            Complex.of(-0.25, -6.0)
    };

    /** Asserts that two complex numbers agree to a relative tolerance. */
    private static void assertClose(Complex expected, Complex actual, String context) {
        double scale = Math.max(expected.magnitude(), Double.MIN_NORMAL);
        double error = expected.subtract(actual).magnitude() / scale;

        assertTrue(error <= RELATIVE_TOLERANCE,
                context + ": expected " + expected + " but was " + actual
                        + " (relative error " + error + ")");
    }

    private static void assertClose(double expected, Complex actual, String context) {
        assertClose(Complex.ofReal(expected), actual, context);
    }

    @Nested
    @DisplayName("Values known in closed form")
    class KnownValues {

        @Test
        @DisplayName("On the positive integers gamma is the shifted factorial")
        void factorialAtIntegers() {
            assertClose(1.0, GammaFunction.gamma(Complex.ofReal(1.0)), "gamma(1) = 0!");
            assertClose(1.0, GammaFunction.gamma(Complex.ofReal(2.0)), "gamma(2) = 1!");
            assertClose(2.0, GammaFunction.gamma(Complex.ofReal(3.0)), "gamma(3) = 2!");
            assertClose(6.0, GammaFunction.gamma(Complex.ofReal(4.0)), "gamma(4) = 3!");
            assertClose(24.0, GammaFunction.gamma(Complex.ofReal(5.0)), "gamma(5) = 4!");
            assertClose(362880.0, GammaFunction.gamma(Complex.ofReal(10.0)), "gamma(10) = 9!");
        }

        @Test
        @DisplayName("At one half gamma is the square root of pi")
        void gammaAtOneHalf() {
            assertClose(SQRT_PI, GammaFunction.gamma(Complex.ofReal(0.5)), "gamma(1/2)");
            assertClose(SQRT_PI / 2.0, GammaFunction.gamma(Complex.ofReal(1.5)), "gamma(3/2)");
        }

        @Test
        @DisplayName("Gamma is defined and negative at minus one half")
        void gammaAtNegativeOneHalf() {
            assertClose(-2.0 * SQRT_PI,
                    GammaFunction.gamma(Complex.ofReal(-0.5)), "gamma(-1/2)");
        }

        @Test
        @DisplayName("Gamma of one plus i matches the published value")
        void gammaAtOnePlusI() {
            Complex expected = Complex.of(0.49801566811835604, -0.15494982830181069);

            assertClose(expected, GammaFunction.gamma(Complex.of(1.0, 1.0)), "gamma(1+i)");
        }

        @Test
        @DisplayName("Gamma is real on the real axis")
        void realOnTheRealAxis() {
            for (double x : new double[] {0.25, 1.0, 2.5, 10.0, -0.5, -3.7}) {
                Complex value = GammaFunction.gamma(Complex.ofReal(x));

                assertTrue(Math.abs(value.imaginary()) <= 1e-15 * Math.abs(value.real()),
                        "gamma(" + x + ") should be real, was " + value);
            }
        }
    }

    @Nested
    @DisplayName("Defining identities")
    class Identities {

        @Test
        @DisplayName("The recurrence gamma(z+1) = z * gamma(z) holds everywhere")
        void recurrence() {
            for (Complex z : SAMPLES) {
                Complex shifted = GammaFunction.gamma(z.add(Complex.ONE));
                Complex scaled = z.multiply(GammaFunction.gamma(z));

                assertClose(shifted, scaled, "recurrence at " + z);
            }
        }

        @Test
        @DisplayName("The reflection formula gamma(z)gamma(1-z) = pi/sin(pi z) holds everywhere")
        void reflection() {
            for (Complex z : SAMPLES) {
                Complex product = GammaFunction.gamma(z)
                        .multiply(GammaFunction.gamma(Complex.ONE.subtract(z)));
                Complex expected = Complex.ofReal(Math.PI)
                        .divide(ComplexMath.sin(z.multiply(Complex.ofReal(Math.PI))));

                assertClose(expected, product, "reflection at " + z);
            }
        }

        @Test
        @DisplayName("Gamma of the conjugate is the conjugate of gamma")
        void conjugateSymmetry() {
            for (Complex z : SAMPLES) {
                Complex viaConjugateFirst = GammaFunction.gamma(z.conjugate());
                Complex viaGammaFirst = GammaFunction.gamma(z).conjugate();

                assertClose(viaConjugateFirst, viaGammaFirst, "conjugate symmetry at " + z);
            }
        }
    }

    @Nested
    @DisplayName("Poles")
    class Poles {

        @Test
        @DisplayName("Gamma has poles at zero and at every negative integer")
        void polesAreRejected() {
            for (double pole : new double[] {0.0, -1.0, -2.0, -5.0, -20.0}) {
                assertThrows(ArithmeticException.class,
                        () -> GammaFunction.gamma(Complex.ofReal(pole)),
                        "expected a pole at s = " + pole);
            }
        }

        @Test
        @DisplayName("A negative integer with a non-zero imaginary part is not a pole")
        void justOffAPoleIsFinite() {
            Complex justOff = Complex.of(-3.0, 1e-3);
            Complex value = GammaFunction.gamma(justOff);

            assertTrue(Double.isFinite(value.real()) && Double.isFinite(value.imaginary()),
                    "gamma should be finite just off the pole, was " + value);
            assertTrue(value.magnitude() > 100.0,
                    "gamma should be large near a pole, was " + value.magnitude());
        }
    }

    @Nested
    @DisplayName("Behaviour on the critical line")
    class CriticalLine {

        @Test
        @DisplayName("Gamma decays extremely fast as the height grows")
        void decaysWithHeight() {
            double atTen = GammaFunction.gamma(Complex.of(0.5, 10.0)).magnitude();
            double atTwenty = GammaFunction.gamma(Complex.of(0.5, 20.0)).magnitude();

            assertTrue(atTwenty < atTen / 1000.0,
                    "expected fast decay, got " + atTen + " then " + atTwenty);
        }

        @Test
        @DisplayName("The magnitude on the critical line matches the exact identity")
        void magnitudeMatchesExactIdentity() {
            // |gamma(1/2 + it)|^2 = gamma(1/2+it) * gamma(1/2-it), because the
            // two are conjugates, and the reflection formula turns that into
            // pi / cosh(pi t). An exact identity rather than an approximation,
            // which makes it a sharp test of the implementation - and a useful
            // one, because it is the factor that will make the functional
            // equation numerically delicate at large heights.
            for (double t : new double[] {0.5, 3.0, 8.0, 14.134725141734693, 25.0}) {
                double computed = GammaFunction.gamma(Complex.of(0.5, t)).magnitude();
                double exact = Math.sqrt(Math.PI / Math.cosh(Math.PI * t));

                assertEquals(1.0, computed / exact, 1e-11,
                        "at t = " + t + ": computed " + computed + ", exact " + exact);
            }
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Null is rejected")
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> GammaFunction.gamma(null));
        }
    }
    @Test
    @DisplayName("Large arguments stay finite up to where gamma itself overflows")
    void largeArgumentsDoNotOverflowEarly() {
        // 170! = 7.2574e306 is the largest factorial a double can hold.
        // Computing t^(w+1/2) and e^(-t) as separate factors fails near
        // z = 142, long before that, because the first overflows while the
        // second underflows - and the product of Infinity with a finite
        // number is Infinity, so the complex multiply returns NaN.
        Complex atOneSeventyOne = GammaFunction.gamma(Complex.ofReal(171.0));

        assertTrue(Double.isFinite(atOneSeventyOne.real()),
                "gamma(171) = 170! is representable and must not come back "
                        + atOneSeventyOne);
        assertEquals(1.0, atOneSeventyOne.real() / 7.257415615307999e306, 1e-12,
                "gamma(171) should be 170!");

        for (double x : new double[] {145.0, 160.0, 170.0}) {
            assertTrue(Double.isFinite(GammaFunction.gamma(Complex.ofReal(x)).real()),
                    "gamma(" + x + ") came back non-finite");
        }
    }
}