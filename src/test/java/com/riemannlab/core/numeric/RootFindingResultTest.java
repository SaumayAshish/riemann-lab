package com.riemannlab.core.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the report a root finder hands back: the final iterate, the cost,
 * and why it stopped.
 *
 * <p>"Converged" is not a single question. A residual below tolerance means the
 * function is nearly zero at that point; a step below tolerance means further
 * iterations cannot improve the answer. Both are success, they are different
 * facts, and the result records which one occurred.</p>
 */
class RootFindingResultTest {

    private static final List<Complex> THREE_ITERATES = List.of(
            Complex.ofReal(3.0),
            Complex.ofReal(1.8333333333333333),
            Complex.ofReal(1.4621212121212122));

    @Nested
    @DisplayName("Reading the outcome")
    class Outcome {

        @Test
        @DisplayName("The root is the last iterate")
        void rootIsTheLastIterate() {
            RootFindingResult result = new RootFindingResult(
                    THREE_ITERATES, 0.1378,
                    RootFindingResult.Termination.STEP_BELOW_TOLERANCE);

            assertEquals(1.4621212121212122, result.root().real(), 1e-15);
        }

        @Test
        @DisplayName("Iteration count is one fewer than the number of iterates")
        void iterationCountExcludesTheStartingPoint() {
            RootFindingResult result = new RootFindingResult(
                    THREE_ITERATES, 0.1378,
                    RootFindingResult.Termination.MAX_ITERATIONS_REACHED);

            assertEquals(2, result.iterations(),
                    "three iterates means two steps were taken");
        }

        @Test
        @DisplayName("A single iterate means no steps were taken")
        void singleIterateMeansZeroIterations() {
            RootFindingResult result = new RootFindingResult(
                    List.of(Complex.ofReal(1.4142135623730951)), 4.4e-16,
                    RootFindingResult.Termination.RESIDUAL_BELOW_TOLERANCE);

            assertEquals(0, result.iterations());
            assertTrue(result.converged(), "the starting point was already a root");
        }
    }

    @Nested
    @DisplayName("Which terminations count as success")
    class Success {

        @Test
        @DisplayName("A small residual is success")
        void residualBelowToleranceIsSuccess() {
            assertTrue(RootFindingResult.Termination.RESIDUAL_BELOW_TOLERANCE.isSuccess());
        }

        @Test
        @DisplayName("A step that has stopped moving is also success")
        void stepBelowToleranceIsSuccess() {
            assertTrue(RootFindingResult.Termination.STEP_BELOW_TOLERANCE.isSuccess());
        }

        @Test
        @DisplayName("Running out of iterations is not success")
        void maxIterationsIsNotSuccess() {
            assertFalse(RootFindingResult.Termination.MAX_ITERATIONS_REACHED.isSuccess());
        }

        @Test
        @DisplayName("A vanished derivative is not success")
        void vanishedDerivativeIsNotSuccess() {
            assertFalse(RootFindingResult.Termination.DERIVATIVE_VANISHED.isSuccess());
        }

        @Test
        @DisplayName("Divergence is not success")
        void divergenceIsNotSuccess() {
            assertFalse(RootFindingResult.Termination.DIVERGED.isSuccess());
        }

        @Test
        @DisplayName("converged() follows the termination reason")
        void convergedFollowsTermination() {
            for (RootFindingResult.Termination termination
                    : RootFindingResult.Termination.values()) {
                RootFindingResult result =
                        new RootFindingResult(THREE_ITERATES, 1e-3, termination);

                assertEquals(termination.isSuccess(), result.converged(),
                        "mismatch for " + termination);
            }
        }
    }

    @Nested
    @DisplayName("Immutability and validation")
    class Validation {

        @Test
        @DisplayName("The iterate list cannot be modified through the result")
        void iteratesAreUnmodifiable() {
            RootFindingResult result = new RootFindingResult(
                    THREE_ITERATES, 0.1,
                    RootFindingResult.Termination.MAX_ITERATIONS_REACHED);

            assertThrows(UnsupportedOperationException.class,
                    () -> result.iterates().clear());
        }

        @Test
        @DisplayName("An empty iterate list is rejected")
        void rejectsEmptyIterates() {
            assertThrows(IllegalArgumentException.class,
                    () -> new RootFindingResult(List.of(), 0.1,
                            RootFindingResult.Termination.DIVERGED));
        }

        @Test
        @DisplayName("A negative residual is rejected")
        void rejectsNegativeResidual() {
            assertThrows(IllegalArgumentException.class,
                    () -> new RootFindingResult(THREE_ITERATES, -1.0,
                            RootFindingResult.Termination.DIVERGED));
        }

        @Test
        @DisplayName("A null termination reason is rejected")
        void rejectsNullTermination() {
            assertThrows(NullPointerException.class,
                    () -> new RootFindingResult(THREE_ITERATES, 0.1, null));
        }
    }
}
