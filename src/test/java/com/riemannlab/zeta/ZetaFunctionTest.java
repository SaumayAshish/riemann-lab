package com.riemannlab.zeta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests the static facade.
 *
 * <p>A facade has one job: delegate. So these tests check that it delegates to
 * the default evaluator faithfully and adds nothing of its own. The numerical
 * behaviour itself is verified where it lives, in
 * {@link ZetaEvaluatorContractTest} and {@link AcceleratedEtaEvaluatorTest}.</p>
 *
 * <p>Testing the facade's delegation separately from the mathematics is what
 * keeps this file short. If it were long, the facade would be doing too
 * much.</p>
 */
class ZetaFunctionTest {

    private static final double ZETA_AT_TWO = Math.PI * Math.PI / 6.0;
    private static final double FIRST_ZERO = 14.134725141734693;

    @Nested
    @DisplayName("Delegation")
    class Delegation {

        @Test
        @DisplayName("The convenience method returns exactly what the default evaluator returns")
        void convenienceMethodMatchesTheDefaultEvaluator() {
            Complex s = Complex.of(0.5, 9.25);

            Complex viaFacade = ZetaFunction.evaluate(s);
            Complex viaEvaluator = ZetaFunction.defaultEvaluator().valueAt(s);

            assertEquals(viaEvaluator.real(), viaFacade.real(), 0.0);
            assertEquals(viaEvaluator.imaginary(), viaFacade.imaginary(), 0.0);
        }

        @Test
        @DisplayName("Diagnostics pass through unchanged")
        void diagnosticsPassThrough() {
            Complex s = Complex.of(0.5, 9.25);

            ZetaResult viaFacade = ZetaFunction.evaluateWithDiagnostics(s);
            ZetaResult viaEvaluator = ZetaFunction.defaultEvaluator().evaluate(s);

            assertEquals(viaEvaluator.termsUsed(), viaFacade.termsUsed());
            assertEquals(viaEvaluator.estimatedErrorBound(), viaFacade.estimatedErrorBound(), 0.0);
        }

        @Test
        @DisplayName("The default evaluator is the accelerated one")
        void defaultIsAccelerated() {
            assertNotNull(ZetaFunction.defaultEvaluator());
            assertTrue(ZetaFunction.defaultEvaluator() instanceof AcceleratedEtaEvaluator,
                    "the default should be the accurate evaluator, was "
                            + ZetaFunction.defaultEvaluator().name());
        }

        @Test
        @DisplayName("The same evaluator instance is reused rather than rebuilt per call")
        void defaultEvaluatorIsShared() {
            assertTrue(ZetaFunction.defaultEvaluator() == ZetaFunction.defaultEvaluator(),
                    "the default evaluator should be a single shared instance");
        }
    }

    @Nested
    @DisplayName("Behaviour inherited from the default evaluator")
    class InheritedBehaviour {

        @Test
        @DisplayName("zeta(2) = pi^2/6")
        void zetaAtTwo() {
            assertEquals(ZETA_AT_TWO, ZetaFunction.evaluate(Complex.ofReal(2)).real(), 1e-13);
        }

        @Test
        @DisplayName("A known zero evaluates to essentially zero")
        void knownZeroVanishes() {
            double magnitude = ZetaFunction.evaluate(Complex.of(0.5, FIRST_ZERO)).magnitude();
            assertTrue(magnitude < 1e-9, "|zeta| at the first zero was " + magnitude);
        }

        @Test
        @DisplayName("The pole at s = 1 is still rejected through the facade")
        void rejectsThePole() {
            assertThrows(ArithmeticException.class,
                    () -> ZetaFunction.evaluate(Complex.ONE));
        }

        @Test
        @DisplayName("Arguments outside the eta domain are still rejected through the facade")
        void rejectsOutsideDomain() {
            assertThrows(IllegalArgumentException.class,
                    () -> ZetaFunction.evaluate(Complex.of(-2, 1)));
        }
    }
}
