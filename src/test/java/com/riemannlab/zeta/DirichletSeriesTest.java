package com.riemannlab.zeta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Characterises the defining Dirichlet series of the Riemann zeta function,
 * {@code sum 1/n^s}, including the two ways it fails.
 *
 * <p>These tests are as much documentation as verification. They pin down, in
 * executable form, why the defining series cannot be used to locate zeros:
 * it converges far too slowly where it converges at all, and on the critical
 * line it does not converge.</p>
 */
class DirichletSeriesTest {

    private static final double TOLERANCE = 1e-12;

    /** Euler's solution to the Basel problem: zeta(2) = pi^2 / 6. */
    private static final double BASEL_CONSTANT = Math.PI * Math.PI / 6.0;

    /** Imaginary part of the first non-trivial zero. A published reference value. */
    private static final double FIRST_ZERO_HEIGHT = 14.134725141734693;

    @Nested
    @DisplayName("The definition itself")
    class Definition {

        @Test
        @DisplayName("A single term is 1, because 1^-s = 1 for every s")
        void firstTermIsAlwaysOne() {
            Complex[] samples = {
                Complex.ofReal(2), Complex.of(0.5, 14.0), Complex.of(-3, 7)
            };
            for (Complex s : samples) {
                Complex sum = DirichletSeries.partialSum(s, 1);
                assertEquals(1.0, sum.real(), TOLERANCE, "real part for s = " + s);
                assertEquals(0.0, sum.imaginary(), TOLERANCE, "imaginary part for s = " + s);
            }
        }

        @Test
        @DisplayName("Ten terms of sum 1/n^2 equal 1.5497677311665408, checkable by hand")
        void tenTermsOfTheBaselSeries() {
            Complex sum = DirichletSeries.partialSum(Complex.ofReal(2), 10);
            assertEquals(1.5497677311665408, sum.real(), 1e-12);
            assertEquals(0.0, sum.imaginary(), TOLERANCE,
                    "a real exponent must produce a real result");
        }

        @Test
        @DisplayName("A real s produces a real result - no imaginary part appears from nowhere")
        void realExponentGivesRealResult() {
            Complex sum = DirichletSeries.partialSum(Complex.ofReal(3), 500);
            assertEquals(0.0, sum.imaginary(), TOLERANCE);
            assertEquals(1.2020569031595943, sum.real(), 1e-5,
                    "should be approaching Apery's constant");
        }
    }

    @Nested
    @DisplayName("Convergence when Re(s) > 1")
    class ConvergenceAboveOne {

        @Test
        @DisplayName("sum 1/n^2 approaches pi^2/6")
        void baselSeriesApproachesPiSquaredOverSix() {
            Complex sum = DirichletSeries.partialSum(Complex.ofReal(2), 100_000);
            assertEquals(BASEL_CONSTANT, sum.real(), 1e-4);
        }

        @Test
        @DisplayName("The error shrinks as more terms are added")
        void errorDecreasesWithMoreTerms() {
            double errorAt10 = baselError(10);
            double errorAt100 = baselError(100);
            double errorAt1000 = baselError(1_000);

            assertTrue(errorAt100 < errorAt10,
                    "error at 100 terms (" + errorAt100 + ") should beat 10 terms (" + errorAt10 + ")");
            assertTrue(errorAt1000 < errorAt100,
                    "error at 1000 terms (" + errorAt1000 + ") should beat 100 terms (" + errorAt100 + ")");
        }

        @Test
        @DisplayName("Convergence is punishingly slow: the error tracks 1/N, not 1/2^N")
        void convergenceIsOnlyFirstOrder() {
            double errorAt1000 = baselError(1_000);

            assertTrue(errorAt1000 > 1e-4,
                    "a thousand terms must still leave an error above 1e-4; it was " + errorAt1000
                            + ". Slow convergence is the whole point of this test.");
            assertTrue(errorAt1000 < 1e-2,
                    "but the error should be roughly 1/N = 1e-3; it was " + errorAt1000);
        }

        private double baselError(int termCount) {
            Complex sum = DirichletSeries.partialSum(Complex.ofReal(2), termCount);
            return Math.abs(BASEL_CONSTANT - sum.real());
        }
    }

    @Nested
    @DisplayName("Divergence when Re(s) <= 1")
    class DivergenceAtOrBelowOne {

        @Test
        @DisplayName("s = 1 is the harmonic series, which grows without bound")
        void harmonicSeriesDiverges() {
            double sumAt1000 = DirichletSeries.partialSum(Complex.ONE, 1_000).real();
            double sumAt10000 = DirichletSeries.partialSum(Complex.ONE, 10_000).real();

            assertTrue(sumAt1000 > 7.0, "H_1000 is about 7.485, got " + sumAt1000);
            assertTrue(sumAt10000 > 9.0, "H_10000 is about 9.788, got " + sumAt10000);
            assertTrue(sumAt10000 > sumAt1000, "the partial sums must keep growing");
        }

        @Test
        @DisplayName("The harmonic partial sums grow like ln(N), so they never settle")
        void harmonicGrowthIsLogarithmic() {
            double sumAt10000 = DirichletSeries.partialSum(Complex.ONE, 10_000).real();
            double eulerMascheroni = 0.5772156649015329;
            assertEquals(Math.log(10_000) + eulerMascheroni, sumAt10000, 1e-3,
                    "H_N should track ln(N) + gamma");
        }

        @Test
        @DisplayName("On the critical line the partial sums diverge - magnitude grows like sqrt(N)")
        void criticalLineSeriesDiverges() {
            Complex s = Complex.of(0.5, FIRST_ZERO_HEIGHT);

            double magnitudeAt1000 = DirichletSeries.partialSum(s, 1_000).magnitude();
            double magnitudeAt100000 = DirichletSeries.partialSum(s, 100_000).magnitude();

            assertTrue(magnitudeAt100000 > 5.0,
                    "after 100000 terms the partial sum magnitude was " + magnitudeAt100000
                            + "; the series is diverging, not settling near the true value of ~0");
            assertTrue(magnitudeAt100000 > magnitudeAt1000,
                    "magnitude must keep growing: " + magnitudeAt1000 + " -> " + magnitudeAt100000);
        }

        @Test
        @DisplayName("The defining series cannot find the first zero, which is the whole problem")
        void theSeriesDoesNotSeeTheZero() {
            Complex firstZero = Complex.of(0.5, FIRST_ZERO_HEIGHT);
            double magnitude = DirichletSeries.partialSum(firstZero, 50_000).magnitude();

            assertTrue(magnitude > 1.0,
                    "zeta(0.5 + 14.134725i) is zero, but the defining series reports magnitude "
                            + magnitude + ". This is why the project needs analytic continuation.");
        }
    }

    @Nested
    @DisplayName("Input validation")
    class Validation {

        @Test
        @DisplayName("A term count below one is rejected")
        void rejectsNonPositiveTermCount() {
            Complex s = Complex.ofReal(2);
            assertThrows(IllegalArgumentException.class, () -> DirichletSeries.partialSum(s, 0));
            assertThrows(IllegalArgumentException.class, () -> DirichletSeries.partialSum(s, -1));
        }
    }
}
