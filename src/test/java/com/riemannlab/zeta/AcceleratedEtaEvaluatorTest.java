package com.riemannlab.zeta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests specific to the accelerated evaluator: the accuracy it claims, the
 * order it selects, and the height beyond which it refuses to answer.
 *
 * <p>Behaviour shared with every evaluator lives in
 * {@link ZetaEvaluatorContractTest} instead of being duplicated here.</p>
 *
 * <p>Zero heights are published reference values used for validation, not
 * discoveries. Agreement with them is numerical evidence, never proof.</p>
 */
class AcceleratedEtaEvaluatorTest {

    private static final double ZETA_AT_TWO = Math.PI * Math.PI / 6.0;
    private static final double ZETA_AT_FOUR = Math.pow(Math.PI, 4) / 90.0;
    private static final double ZETA_AT_HALF = -1.4603545088095868;

    private static final double FIRST_ZERO = 14.134725141734693;
    private static final double SECOND_ZERO = 21.022039638771555;
    private static final double THIRD_ZERO = 25.010857580145688;

    private final AcceleratedEtaEvaluator evaluator = new AcceleratedEtaEvaluator();

    @Nested
    @DisplayName("Machine-precision accuracy")
    class Accuracy {

        @Test
        @DisplayName("zeta(2) = pi^2/6 to near machine precision")
        void zetaAtTwo() {
            assertEquals(ZETA_AT_TWO, evaluator.valueAt(Complex.ofReal(2)).real(), 1e-13);
        }

        @Test
        @DisplayName("zeta(4) = pi^4/90 to near machine precision")
        void zetaAtFour() {
            assertEquals(ZETA_AT_FOUR, evaluator.valueAt(Complex.ofReal(4)).real(), 1e-13);
        }

        @Test
        @DisplayName("zeta(1/2) to near machine precision, inside the critical strip")
        void zetaAtHalf() {
            assertEquals(ZETA_AT_HALF, evaluator.valueAt(Complex.ofReal(0.5)).real(), 1e-12);
        }

        @Test
        @DisplayName("The first three known zeros evaluate below 1e-9")
        void knownZerosVanish() {
            for (double height : new double[] {FIRST_ZERO, SECOND_ZERO, THIRD_ZERO}) {
                double magnitude = evaluator.valueAt(Complex.of(0.5, height)).magnitude();
                assertTrue(magnitude < 1e-9,
                        "|zeta(0.5 + " + height + "i)| was " + magnitude);
            }
        }

        @Test
        @DisplayName("Uses a few dozen terms, not tens of thousands")
        void usesFewTerms() {
            ZetaResult result = evaluator.evaluate(Complex.of(0.5, FIRST_ZERO));
            assertTrue(result.termsUsed() < 100,
                    "expected a few dozen terms, used " + result.termsUsed());
        }

        @Test
        @DisplayName("Zeta is conjugate-symmetric about the real axis")
        void obeysConjugateSymmetry() {
            Complex above = evaluator.valueAt(Complex.of(0.5, 12.0));
            Complex below = evaluator.valueAt(Complex.of(0.5, -12.0));

            assertEquals(above.real(), below.real(), 1e-12);
            assertEquals(above.imaginary(), -below.imaginary(), 1e-12);
        }
    }

    @Nested
    @DisplayName("Agreement with independent methods")
    class CrossValidation {

        @Test
        @DisplayName("Agrees with the defining Dirichlet series where that series is valid")
        void agreesWithDefiningSeries() {
            Complex s = Complex.of(4, 1.5);

            Complex accelerated = evaluator.valueAt(s);
            Complex viaSeries = DirichletSeries.partialSum(s, 200_000);

            assertEquals(viaSeries.real(), accelerated.real(), 1e-8);
            assertEquals(viaSeries.imaginary(), accelerated.imaginary(), 1e-8);
        }

        @Test
        @DisplayName("Agrees with the naive evaluator, to the naive one's accuracy")
        void agreesWithNaiveEvaluator() {
            Complex s = Complex.of(0.5, 10.0);

            Complex fast = evaluator.valueAt(s);
            Complex slow = new NaiveEtaEvaluator(50_000).valueAt(s);

            assertEquals(slow.real(), fast.real(), 1e-2);
            assertEquals(slow.imaginary(), fast.imaginary(), 1e-2);
        }

        @Test
        @DisplayName("Beats the naive evaluator by many orders of magnitude at a zero")
        void decisivelyBeatsTheNaiveEvaluator() {
            Complex s = Complex.of(0.5, FIRST_ZERO);

            double fast = evaluator.valueAt(s).magnitude();
            double slow = new NaiveEtaEvaluator(20_000).valueAt(s).magnitude();

            assertTrue(slow > 1e-4, "naive evaluator was unexpectedly good: " + slow);
            assertTrue(fast < slow / 1e6,
                    "accelerated (" + fast + ") should crush naive (" + slow + ")");
        }
    }

    @Nested
    @DisplayName("Order selection and the height ceiling")
    class OrderSelection {

        @Test
        @DisplayName("Order grows with the imaginary part")
        void orderGrowsWithHeight() {
            int atZero = evaluator.orderFor(Complex.ofReal(0.5));
            int atTwenty = evaluator.orderFor(Complex.of(0.5, 20));
            int atHundred = evaluator.orderFor(Complex.of(0.5, 100));

            assertTrue(atTwenty > atZero, atZero + " -> " + atTwenty);
            assertTrue(atHundred > atTwenty, atTwenty + " -> " + atHundred);
        }

        @Test
        @DisplayName("Order ignores the sign of the height")
        void orderIsSymmetricInSign() {
            assertEquals(evaluator.orderFor(Complex.of(0.5, 30)),
                    evaluator.orderFor(Complex.of(0.5, -30)));
        }

        @Test
        @DisplayName("Heights beyond the ceiling are refused rather than approximated badly")
        void refusesExcessiveHeight() {
            Complex tooHigh = Complex.of(0.5, 5_000.0);

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> evaluator.evaluate(tooHigh));

            assertTrue(thrown.getMessage().contains("Riemann"),
                    "the message should say what would be needed instead, was: "
                            + thrown.getMessage());
        }

        @Test
        @DisplayName("A height of 150 is still comfortably supported")
        void supportsModerateHeights() {
            Complex result = evaluator.valueAt(Complex.of(0.5, 150.0));
            assertTrue(Double.isFinite(result.real()) && Double.isFinite(result.imaginary()));
        }

        @Test
        @DisplayName("The reported ceiling matches the point at which evaluation is refused")
        void ceilingIsConsistentWithRefusal() {
            double ceiling = evaluator.maxSupportedHeight();

            Complex justUnder = Complex.of(0.5, ceiling - 1.0);
            Complex justOver = Complex.of(0.5, ceiling + 1.0);

            evaluator.evaluate(justUnder);
            assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(justOver));
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("A higher base order gives a tighter claimed error bound")
        void higherBaseOrderTightensTheBound() {
            ZetaResult modest = new AcceleratedEtaEvaluator(8, 1.2)
                    .evaluate(Complex.ofReal(2));
            ZetaResult generous = new AcceleratedEtaEvaluator(25, 1.2)
                    .evaluate(Complex.ofReal(2));

            assertTrue(generous.estimatedErrorBound() < modest.estimatedErrorBound(),
                    modest.estimatedErrorBound() + " -> " + generous.estimatedErrorBound());
        }

        @Test
        @DisplayName("A nonsensical configuration is rejected at construction")
        void rejectsBadConfiguration() {
            assertThrows(IllegalArgumentException.class,
                    () -> new AcceleratedEtaEvaluator(0, 1.2));
            assertThrows(IllegalArgumentException.class,
                    () -> new AcceleratedEtaEvaluator(25, -0.5));
        }

        @Test
        @DisplayName("The name reflects the configuration, for logs and reports")
        void nameDescribesTheConfiguration() {
            String name = new AcceleratedEtaEvaluator(25, 1.2).name();
            assertTrue(name.toLowerCase().contains("accelerated"), name);
        }
    }
}
