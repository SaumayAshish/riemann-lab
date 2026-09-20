package com.riemannlab.core.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import java.util.function.IntFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Cohen-Rodriguez Villegas-Zagier acceleration algorithm on
 * alternating series whose limits are known in closed form.
 *
 * <p>The accelerator knows nothing about the zeta function; it is tested here
 * on the alternating harmonic series and the Leibniz series so that failures
 * point at the algorithm rather than at anything downstream of it.</p>
 */
class AlternatingSeriesAcceleratorTest {

    /** 1 - 1/2 + 1/3 - ... = ln 2. */
    private static final IntFunction<Complex> ALTERNATING_HARMONIC =
            k -> Complex.ofReal(1.0 / (k + 1));

    /** 1 - 1/3 + 1/5 - ... = pi/4, the Leibniz series. */
    private static final IntFunction<Complex> LEIBNIZ =
            k -> Complex.ofReal(1.0 / (2.0 * k + 1.0));

    private static final double LOG_TWO = 0.6931471805599453;
    private static final double PI_OVER_FOUR = Math.PI / 4.0;

    @Nested
    @DisplayName("Hand-checkable small orders")
    class SmallOrders {

        @Test
        @DisplayName("Order 2 on the alternating harmonic series gives exactly 12/17")
        void orderTwoMatchesHandCalculation() {
            Complex result = AlternatingSeriesAccelerator.sum(ALTERNATING_HARMONIC, 2);
            assertEquals(12.0 / 17.0, result.real(), 1e-14,
                    "d = 17, sum = 12, so the result must be 12/17");
            assertEquals(0.0, result.imaginary(), 1e-15);
        }

        @Test
        @DisplayName("Order 3 already beats 1000 naive terms")
        void orderThreeBeatsAThousandNaiveTerms() {
            double acceleratedError = Math.abs(LOG_TWO
                    - AlternatingSeriesAccelerator.sum(ALTERNATING_HARMONIC, 3).real());

            double naiveError = Math.abs(LOG_TWO - naiveAlternatingSum(1_000));

            assertTrue(acceleratedError < naiveError,
                    "3 accelerated terms (" + acceleratedError + ") should beat 1000 naive terms ("
                            + naiveError + ")");
        }

        private double naiveAlternatingSum(int termCount) {
            double sum = 0.0;
            for (int k = 0; k < termCount; k++) {
                sum += (k % 2 == 0 ? 1.0 : -1.0) / (k + 1);
            }
            return sum;
        }
    }

    @Nested
    @DisplayName("Convergence to known constants")
    class KnownConstants {

        @Test
        @DisplayName("Order 25 reaches ln 2 to machine precision")
        void reachesLogTwo() {
            Complex result = AlternatingSeriesAccelerator.sum(ALTERNATING_HARMONIC, 25);
            assertEquals(LOG_TWO, result.real(), 1e-14);
        }

        @Test
        @DisplayName("Order 30 reaches pi/4 from the Leibniz series")
        void reachesPiOverFour() {
            Complex result = AlternatingSeriesAccelerator.sum(LEIBNIZ, 30);
            assertEquals(PI_OVER_FOUR, result.real(), 1e-13);
        }

        @Test
        @DisplayName("Error shrinks geometrically with order")
        void errorShrinksGeometrically() {
            double errorAt5 = logTwoError(5);
            double errorAt10 = logTwoError(10);
            double errorAt15 = logTwoError(15);

            assertTrue(errorAt10 < errorAt5 / 100.0,
                    "five more terms should buy several digits: " + errorAt5 + " -> " + errorAt10);
            assertTrue(errorAt15 < errorAt10 / 100.0,
                    "and again: " + errorAt10 + " -> " + errorAt15);
        }

        @Test
        @DisplayName("The published error bound is respected")
        void staysInsideTheStatedErrorBound() {
            for (int order : new int[] {3, 5, 8, 12}) {
                double error = logTwoError(order);
                double bound = AlternatingSeriesAccelerator.errorBound(order);
                assertTrue(error <= bound,
                        "order " + order + ": error " + error + " exceeded bound " + bound);
            }
        }

        private double logTwoError(int order) {
            return Math.abs(LOG_TWO
                    - AlternatingSeriesAccelerator.sum(ALTERNATING_HARMONIC, order).real());
        }
    }

    @Nested
    @DisplayName("Numerical safety")
    class NumericalSafety {

        @Test
        @DisplayName("High orders do not overflow, despite an enormous normaliser")
        void highOrdersRemainFinite() {
            Complex result = AlternatingSeriesAccelerator.sum(ALTERNATING_HARMONIC, 200);

            assertTrue(Double.isFinite(result.real()),
                    "at order 200 the normaliser exceeds 1e150; squaring it would overflow."
                            + " Result was " + result.real());
            assertEquals(LOG_TWO, result.real(), 1e-12,
                    "and the answer must still be correct");
        }

        @Test
        @DisplayName("The error bound decreases by a factor of about 5.83 per order")
        void errorBoundHasTheExpectedRatio() {
            double ratio = AlternatingSeriesAccelerator.errorBound(10)
                    / AlternatingSeriesAccelerator.errorBound(11);
            assertEquals(3.0 + Math.sqrt(8.0), ratio, 1e-9);
        }
    }

    @Nested
    @DisplayName("Input validation")
    class Validation {

        @Test
        @DisplayName("An order below one is rejected")
        void rejectsNonPositiveOrder() {
            assertThrows(IllegalArgumentException.class,
                    () -> AlternatingSeriesAccelerator.sum(ALTERNATING_HARMONIC, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> AlternatingSeriesAccelerator.sum(ALTERNATING_HARMONIC, -3));
        }

        @Test
        @DisplayName("An order beyond the supported maximum is rejected")
        void rejectsExcessiveOrder() {
            int tooLarge = AlternatingSeriesAccelerator.MAX_ORDER + 1;
            assertThrows(IllegalArgumentException.class,
                    () -> AlternatingSeriesAccelerator.sum(ALTERNATING_HARMONIC, tooLarge));
        }
    }
}
