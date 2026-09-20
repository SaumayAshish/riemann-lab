package com.riemannlab.zeta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests specific to the unaccelerated evaluator.
 *
 * <p>This implementation is kept deliberately, despite being far slower and
 * far less accurate than {@link AcceleratedEtaEvaluator}. It sums the eta
 * series directly, so it shares no machinery with the acceleration algorithm
 * and provides genuinely independent confirmation: if the two agree at a
 * point, it is unlikely that both contain the same error.</p>
 *
 * <p>Behaviour common to all evaluators is asserted once, in
 * {@link ZetaEvaluatorContractTest}.</p>
 */
class NaiveEtaEvaluatorTest {

    private static final double ZETA_AT_TWO = Math.PI * Math.PI / 6.0;
    private static final double FIRST_ZERO = 14.134725141734693;

    @Nested
    @DisplayName("Accuracy scales with term count")
    class Accuracy {

        @Test
        @DisplayName("More terms give a smaller error")
        void moreTermsMeansLessError() {
            double errorAt1000 = zetaTwoError(1_000);
            double errorAt100000 = zetaTwoError(100_000);

            assertTrue(errorAt100000 < errorAt1000,
                    errorAt1000 + " -> " + errorAt100000);
        }

        @Test
        @DisplayName("More terms give a tighter claimed bound")
        void moreTermsMeansTighterBound() {
            double boundAt1000 = new NaiveEtaEvaluator(1_000)
                    .evaluate(Complex.ofReal(2)).estimatedErrorBound();
            double boundAt100000 = new NaiveEtaEvaluator(100_000)
                    .evaluate(Complex.ofReal(2)).estimatedErrorBound();

            assertTrue(boundAt100000 < boundAt1000,
                    boundAt1000 + " -> " + boundAt100000);
        }

        @Test
        @DisplayName("On the critical line it manages about three decimal places")
        void criticalLineAccuracyIsAboutThreeDigits() {
            double magnitude = new NaiveEtaEvaluator(20_000)
                    .valueAt(Complex.of(0.5, FIRST_ZERO))
                    .magnitude();

            assertTrue(magnitude > 1e-5,
                    "this evaluator should not be this good; got " + magnitude);
            assertTrue(magnitude < 1e-2,
                    "but it should still find the zero to about 3 places; got " + magnitude);
        }

        @Test
        @DisplayName("It reports the term count it was configured with")
        void reportsItsTermCount() {
            NaiveEtaEvaluator evaluator = new NaiveEtaEvaluator(5_000);

            assertEquals(5_000, evaluator.termCount());
            assertEquals(5_000, evaluator.evaluate(Complex.ofReal(2)).termsUsed());
        }

        private double zetaTwoError(int termCount) {
            return Math.abs(ZETA_AT_TWO
                    - new NaiveEtaEvaluator(termCount).valueAt(Complex.ofReal(2)).real());
        }
    }

    @Nested
    @DisplayName("Independence from the accelerated route")
    class Independence {

        @Test
        @DisplayName("Agrees with the accelerated evaluator at a point away from any zero")
        void agreesWithAcceleratedEvaluator() {
            Complex s = Complex.of(0.5, 17.75);

            Complex naive = new NaiveEtaEvaluator(200_000).valueAt(s);
            Complex accelerated = new AcceleratedEtaEvaluator().valueAt(s);

            assertEquals(accelerated.real(), naive.real(), 5e-3);
            assertEquals(accelerated.imaginary(), naive.imaginary(), 5e-3);
        }

        @Test
        @DisplayName("Agrees with the defining Dirichlet series where that series is valid")
        void agreesWithDefiningSeries() {
            Complex s = Complex.ofReal(3);

            Complex naive = new NaiveEtaEvaluator(100_000).valueAt(s);
            Complex viaSeries = DirichletSeries.partialSum(s, 100_000);

            assertEquals(viaSeries.real(), naive.real(), 1e-6);
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("A term count below one is rejected at construction")
        void rejectsNonPositiveTermCount() {
            assertThrows(IllegalArgumentException.class, () -> new NaiveEtaEvaluator(0));
            assertThrows(IllegalArgumentException.class, () -> new NaiveEtaEvaluator(-1));
        }

        @Test
        @DisplayName("The name records the term count, so reports are unambiguous")
        void nameRecordsTheTermCount() {
            String name = new NaiveEtaEvaluator(20_000).name();
            assertTrue(name.contains("20"), "name should mention the term count, was " + name);
        }
    }
}
