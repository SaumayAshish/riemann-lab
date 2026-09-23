package com.riemannlab.zeta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the analytic continuation of zeta through the functional equation.
 *
 * <p>The strongest test here is {@code bothRoutesAgreeInsideTheStrip}. For
 * {@code 0 < Re(s) < 1/2} the value can be reached two entirely independent
 * ways - directly, by the accelerated eta series, or by reflecting to
 * {@code 1-s} and multiplying by four other factors including a gamma function
 * and a complex sine. Neither route knows anything about the other. Agreement
 * to thirteen digits is not a coincidence that a broken implementation could
 * produce.</p>
 *
 * <p>The trivial zeros are checked but not hard-coded anywhere in the
 * implementation. They arise because the sine factor vanishes at the negative
 * even integers, and the assertion is the project's usual one: the computed
 * magnitude must be smaller than the error of the computation that produced
 * it.</p>
 */
class ContinuedZetaEvaluatorTest {

    /**
     * Slack allowed when comparing two independent routes against the sum of
     * their stated bounds. Both bounds are estimates, so an order of magnitude
     * of room avoids a test that fails on an optimistic model rather than on a
     * real disagreement. The same factor is used by {@code ZeroRefiner}.
     */
    private static final double AGREEMENT_SLACK = 10.0;

    private static final double FIRST_ZERO = 14.134725141734693;

    private final AcceleratedEtaEvaluator base = new AcceleratedEtaEvaluator();
    private final ContinuedZetaEvaluator evaluator = new ContinuedZetaEvaluator(base);

    @Nested
    @DisplayName("Values at the negative integers")
    class NegativeIntegers {

        @Test
        @DisplayName("The negative odd integers give the known rational values")
        void negativeOddIntegersMatchTheKnownValues() {
            double[][] expected = {
                    {-1.0, -1.0 / 12.0},
                    {-3.0, 1.0 / 120.0},
                    {-5.0, -1.0 / 252.0},
                    {-7.0, 1.0 / 240.0}
            };

            for (double[] pair : expected) {
                ZetaResult result = evaluator.evaluate(Complex.ofReal(pair[0]));
                double error = Math.abs(result.value().real() - pair[1]);

                assertTrue(error <= AGREEMENT_SLACK * result.estimatedErrorBound(),
                        "zeta(" + pair[0] + ") = " + result.value()
                                + ", expected " + pair[1] + ", error " + error
                                + " against bound " + result.estimatedErrorBound());
                assertEquals(0.0, result.value().imaginary(), 1e-15,
                        "zeta must be real at a real argument");
            }
        }

        @Test
        @DisplayName("Zeta at minus one half matches the published value")
        void zetaAtMinusOneHalf() {
            ZetaResult result = evaluator.evaluate(Complex.ofReal(-0.5));
            double expected = -0.20788622497735451;
            double error = Math.abs(result.value().real() - expected);

            assertTrue(error <= AGREEMENT_SLACK * result.estimatedErrorBound(),
                    "zeta(-1/2) = " + result.value() + ", error " + error
                            + " against bound " + result.estimatedErrorBound());
        }
    }

    @Nested
    @DisplayName("The trivial zeros")
    class TrivialZeros {

        @Test
        @DisplayName("Every negative even integer is indistinguishable from zero")
        void negativeEvenIntegersAreZeros() {
            for (double s : new double[] {-2.0, -4.0, -6.0, -8.0, -10.0}) {
                ZetaResult result = evaluator.evaluate(Complex.ofReal(s));

                assertTrue(result.isIndistinguishableFromZero(),
                        "zeta(" + s + ") should be a trivial zero: |zeta| = "
                                + result.value().magnitude()
                                + " against bound " + result.estimatedErrorBound());
            }
        }

        @Test
        @DisplayName("The negative odd integers are emphatically not zeros")
        void negativeOddIntegersAreNotZeros() {
            for (double s : new double[] {-1.0, -3.0, -5.0}) {
                ZetaResult result = evaluator.evaluate(Complex.ofReal(s));

                assertFalse(result.isIndistinguishableFromZero(),
                        "zeta(" + s + ") = " + result.value()
                                + " is a known non-zero value and must not be"
                                + " mistaken for a zero");
            }
        }
    }

    @Nested
    @DisplayName("The origin")
    class TheOrigin {

        @Test
        @DisplayName("Zeta at zero is minus one half")
        void zetaAtZeroIsMinusOneHalf() {
            ZetaResult result = evaluator.evaluate(Complex.ZERO);

            assertEquals(-0.5, result.value().real(), 1e-15);
            assertEquals(0.0, result.value().imaginary(), 0.0);
            assertFalse(result.isIndistinguishableFromZero(),
                    "zeta(0) is -1/2, not a zero");
        }
    }

    @Nested
    @DisplayName("Cross-validation against the direct series")
    class Agreement {

        @Test
        @DisplayName("Both routes agree where both are valid")
        void bothRoutesAgreeInsideTheStrip() {
            Complex[] samples = {
                    Complex.of(0.25, 3.0),
                    Complex.of(0.10, 7.0),
                    Complex.of(0.40, 20.0),
                    Complex.of(0.30, -5.0)
            };

            for (Complex s : samples) {
                ZetaResult direct = base.evaluate(s);
                ZetaResult continued = evaluator.evaluate(s);

                double difference =
                        direct.value().subtract(continued.value()).magnitude();
                double allowed = AGREEMENT_SLACK
                        * (direct.estimatedErrorBound() + continued.estimatedErrorBound());

                assertTrue(difference <= allowed,
                        "at " + s + " the eta series gave " + direct.value()
                                + " and the functional equation gave " + continued.value()
                                + "; difference " + difference + " exceeded " + allowed);
            }
        }
    }

    @Nested
    @DisplayName("Delegation to the base evaluator")
    class Delegation {

        @Test
        @DisplayName("At or right of the threshold the base result is passed through unchanged")
        void rightOfTheThresholdIsDelegatedUnchanged() {
            for (Complex s : new Complex[] {
                    Complex.ofReal(2.0), Complex.of(0.5, 8.0), Complex.of(3.0, -2.0)}) {

                ZetaResult delegated = evaluator.evaluate(s);
                ZetaResult direct = base.evaluate(s);

                assertEquals(direct.value().real(), delegated.value().real(), 0.0,
                        "at " + s);
                assertEquals(direct.value().imaginary(), delegated.value().imaginary(), 0.0,
                        "at " + s);
                assertEquals(direct.estimatedErrorBound(), delegated.estimatedErrorBound(),
                        0.0, "the bound must be passed through too, at " + s);
            }
        }

        @Test
        @DisplayName("The critical line is delegated, so earlier phases are unaffected")
        void theCriticalLineIsDelegated() {
            Complex onTheLine = Complex.of(0.5, FIRST_ZERO);

            assertEquals(base.evaluate(onTheLine).value().magnitude(),
                    evaluator.evaluate(onTheLine).value().magnitude(), 0.0,
                    "the zero search was validated against the base evaluator and must"
                            + " keep running on exactly that code");
        }
    }

    @Nested
    @DisplayName("Symmetry forced by the functional equation")
    class Symmetry {

        @Test
        @DisplayName("Reflecting a zero about the critical line gives a zero")
        void reflectionMapsZerosToZeros() {
            Complex rho = Complex.of(0.5, FIRST_ZERO);
            Complex reflected = Complex.ONE.subtract(rho);

            assertTrue(evaluator.evaluate(rho).isIndistinguishableFromZero(),
                    "the starting point must be a zero");
            assertTrue(evaluator.evaluate(reflected).isIndistinguishableFromZero(),
                    "1 - rho = " + reflected + " must also be a zero, because every"
                            + " other factor of the functional equation is non-zero"
                            + " inside the critical strip");
        }

        @Test
        @DisplayName("On the critical line the quadruple of zeros collapses to a pair")
        void theQuadrupleCollapsesOnTheCriticalLine() {
            Complex rho = Complex.of(0.5, FIRST_ZERO);

            Complex conjugate = rho.conjugate();
            Complex reflected = Complex.ONE.subtract(rho);
            Complex reflectedConjugate = Complex.ONE.subtract(conjugate);

            // A zero off the line would give four distinct points. On the line
            // reflection and conjugation coincide, so only two survive - which
            // is exactly what the Riemann Hypothesis asserts happens always,
            // and exactly what this program cannot prove.
            assertEquals(conjugate.real(), reflected.real(), 0.0);
            assertEquals(conjugate.imaginary(), reflected.imaginary(), 0.0);
            assertEquals(rho.real(), reflectedConjugate.real(), 0.0);
            assertEquals(rho.imaginary(), reflectedConjugate.imaginary(), 0.0);

            for (Complex member : new Complex[] {rho, conjugate}) {
                assertTrue(evaluator.evaluate(member).isIndistinguishableFromZero(),
                        member + " must be a zero");
            }
        }
    }

    @Nested
    @DisplayName("Limits of the continuation")
    class Limits {

        @Test
        @DisplayName("Far to the left the continuation is refused rather than overflowing")
        void farToTheLeftIsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> evaluator.evaluate(Complex.ofReal(-200.0)));
            assertThrows(IllegalArgumentException.class,
                    () -> evaluator.evaluate(Complex.of(-500.0, 2.0)));
        }

        @Test
        @DisplayName("Results stay finite and carry an honest bound across the left half-plane")
        void resultsAreFiniteAcrossTheLeftHalfPlane() {
            Complex[] samples = {
                    Complex.of(-1.0, 3.0),
                    Complex.of(-5.0, 0.5),
                    Complex.of(0.2, 10.0),
                    Complex.of(-20.0, 2.0),
                    Complex.of(-100.0, 0.25)
            };

            for (Complex s : samples) {
                ZetaResult result = evaluator.evaluate(s);

                assertTrue(Double.isFinite(result.value().real())
                                && Double.isFinite(result.value().imaginary()),
                        "non-finite value at " + s + ": " + result.value());
                assertTrue(result.estimatedErrorBound() >= 0.0
                                && Double.isFinite(result.estimatedErrorBound()),
                        "unusable bound at " + s + ": " + result.estimatedErrorBound());
                assertTrue(result.termsUsed() >= 1, "termsUsed must be reported at " + s);
            }
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("A null base evaluator is rejected")
        void rejectsNullBase() {
            assertThrows(NullPointerException.class, () -> new ContinuedZetaEvaluator(null));
        }

        @Test
        @DisplayName("A null argument is rejected")
        void rejectsNullArgument() {
            assertThrows(NullPointerException.class, () -> evaluator.evaluate(null));
        }
    }
}