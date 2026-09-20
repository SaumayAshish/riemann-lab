package com.riemannlab.zeta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the eta-based zeta evaluator.
 *
 * <p>Three kinds of check appear here. Known closed-form values such as
 * {@code zeta(2) = pi^2/6} confirm correctness where the answer is provable.
 * Cross-validation against {@link DirichletSeries} confirms that two
 * independent routes agree in the region where both are valid. Finally,
 * published reference values for the non-trivial zeros confirm that the
 * evaluator sees what it should on the critical line.</p>
 *
 * <p>The zero heights used here are established published values, used for
 * validation. They are not discoveries, and agreement with them is numerical
 * evidence rather than proof of anything.</p>
 */
class ZetaFunctionTest {

    private static final int STANDARD_TERMS = 100_000;

    /** zeta(2) = pi^2 / 6, Euler 1735. */
    private static final double ZETA_AT_TWO = Math.PI * Math.PI / 6.0;

    /** zeta(3), Apery's constant. */
    private static final double ZETA_AT_THREE = 1.2020569031595943;

    /** zeta(4) = pi^4 / 90. */
    private static final double ZETA_AT_FOUR = Math.pow(Math.PI, 4) / 90.0;

    /** zeta(1/2), a published reference value. */
    private static final double ZETA_AT_HALF = -1.4603545088095868;

    /** Imaginary parts of the first three non-trivial zeros. Published reference values. */
    private static final double FIRST_ZERO = 14.134725141734693;
    private static final double SECOND_ZERO = 21.022039638771555;
    private static final double THIRD_ZERO = 25.010857580145688;

    /** Spacing of the removable singularities of the eta identity: 2*pi/ln(2). */
    private static final double SINGULARITY_SPACING = 2.0 * Math.PI / Math.log(2.0);

    @Nested
    @DisplayName("Known closed-form values")
    class ClosedFormValues {

        @Test
        @DisplayName("zeta(2) = pi^2/6")
        void zetaAtTwo() {
            Complex result = ZetaFunction.evaluate(Complex.ofReal(2), 10_000);
            assertEquals(ZETA_AT_TWO, result.real(), 1e-6);
            assertEquals(0.0, result.imaginary(), 1e-12);
        }

        @Test
        @DisplayName("zeta(3) = 1.2020569031595943, Apery's constant")
        void zetaAtThree() {
            Complex result = ZetaFunction.evaluate(Complex.ofReal(3), 10_000);
            assertEquals(ZETA_AT_THREE, result.real(), 1e-6);
        }

        @Test
        @DisplayName("zeta(4) = pi^4/90")
        void zetaAtFour() {
            Complex result = ZetaFunction.evaluate(Complex.ofReal(4), 10_000);
            assertEquals(ZETA_AT_FOUR, result.real(), 1e-6);
        }

        @Test
        @DisplayName("zeta(1/2) = -1.4603545, inside the critical strip")
        void zetaAtOneHalf() {
            Complex result = ZetaFunction.evaluate(Complex.ofReal(0.5), STANDARD_TERMS);
            assertEquals(ZETA_AT_HALF, result.real(), 0.05);
            assertTrue(result.real() < 0, "zeta(1/2) is negative, got " + result.real());
        }
    }

    @Nested
    @DisplayName("Cross-validation against the defining series")
    class CrossValidation {

        @Test
        @DisplayName("Both routes agree at s = 3, where each is independently valid")
        void agreesWithDirichletSeriesAboveOne() {
            Complex s = Complex.ofReal(3);

            Complex viaEta = ZetaFunction.evaluate(s, 10_000);
            Complex viaSeries = DirichletSeries.partialSum(s, 100_000);

            assertEquals(viaSeries.real(), viaEta.real(), 1e-6,
                    "two independent methods must agree where both converge");
        }

        @Test
        @DisplayName("Both routes agree at a complex s with Re(s) = 3")
        void agreesWithDirichletSeriesForComplexArgument() {
            Complex s = Complex.of(3, 2);

            Complex viaEta = ZetaFunction.evaluate(s, 10_000);
            Complex viaSeries = DirichletSeries.partialSum(s, 200_000);

            assertEquals(viaSeries.real(), viaEta.real(), 1e-5);
            assertEquals(viaSeries.imaginary(), viaEta.imaginary(), 1e-5);
        }
    }

    @Nested
    @DisplayName("The critical line")
    class CriticalLine {

        @Test
        @DisplayName("The magnitude is small at the first known zero")
        void magnitudeIsSmallAtTheFirstZero() {
            double magnitude = ZetaFunction
                    .evaluate(Complex.of(0.5, FIRST_ZERO), STANDARD_TERMS)
                    .magnitude();

            assertTrue(magnitude < 0.1,
                    "|zeta(0.5 + " + FIRST_ZERO + "i)| should be near zero, was " + magnitude);
        }

        @Test
        @DisplayName("The magnitude is small at the second and third known zeros")
        void magnitudeIsSmallAtLaterZeros() {
            double second = ZetaFunction
                    .evaluate(Complex.of(0.5, SECOND_ZERO), STANDARD_TERMS).magnitude();
            double third = ZetaFunction
                    .evaluate(Complex.of(0.5, THIRD_ZERO), STANDARD_TERMS).magnitude();

            assertTrue(second < 0.1, "second zero magnitude was " + second);
            assertTrue(third < 0.1, "third zero magnitude was " + third);
        }

        @Test
        @DisplayName("The magnitude is clearly larger away from a zero")
        void magnitudeIsLargerBetweenZeros() {
            double atZero = ZetaFunction
                    .evaluate(Complex.of(0.5, FIRST_ZERO), STANDARD_TERMS).magnitude();
            double betweenZeros = ZetaFunction
                    .evaluate(Complex.of(0.5, 17.5), STANDARD_TERMS).magnitude();

            assertTrue(betweenZeros > 10 * atZero,
                    "a zero (" + atZero + ") must stand out against a nearby non-zero ("
                            + betweenZeros + ")");
        }

        @Test
        @DisplayName("The eta route succeeds exactly where the defining series failed")
        void succeedsWhereTheDefiningSeriesDiverged() {
            Complex s = Complex.of(0.5, FIRST_ZERO);

            double viaEta = ZetaFunction.evaluate(s, STANDARD_TERMS).magnitude();
            double viaSeries = DirichletSeries.partialSum(s, STANDARD_TERMS).magnitude();

            assertTrue(viaEta < 0.1, "eta route: " + viaEta);
            assertTrue(viaSeries > 20.0, "defining series: " + viaSeries);
        }
    }

    @Nested
    @DisplayName("Singularities of the identity")
    class Singularities {

        @Test
        @DisplayName("s = 1 is rejected: zeta has a genuine pole there")
        void rejectsThePoleAtOne() {
            assertThrows(ArithmeticException.class,
                    () -> ZetaFunction.evaluate(Complex.ONE, 100));
        }

        @Test
        @DisplayName("s = 1 + 2*pi*i/ln(2) is rejected: the identity has a removable singularity")
        void rejectsRemovableSingularitiesOfTheIdentity() {
            Complex s = Complex.of(1.0, SINGULARITY_SPACING);
            assertThrows(ArithmeticException.class, () -> ZetaFunction.evaluate(s, 100));
        }

        @Test
        @DisplayName("Points on the critical line are never near a singularity")
        void criticalLineIsSafe() {
            for (double t = 0.5; t <= 40.0; t += 0.5) {
                Complex s = Complex.of(0.5, t);
                Complex result = ZetaFunction.evaluate(s, 100);
                assertTrue(Double.isFinite(result.real()) && Double.isFinite(result.imaginary()),
                        "evaluation must remain finite at t = " + t);
            }
        }
    }

    @Nested
    @DisplayName("Input validation")
    class Validation {

        @Test
        @DisplayName("A term count below one is rejected")
        void rejectsNonPositiveTermCount() {
            Complex s = Complex.ofReal(2);
            assertThrows(IllegalArgumentException.class, () -> ZetaFunction.evaluate(s, 0));
        }
    }
}
