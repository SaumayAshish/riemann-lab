package com.riemannlab.core.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Compares the two root finders on the measure that decides which one to use
 * for the zeta function: <strong>function evaluations</strong>, not iterations.
 *
 * <p>Newton converges quadratically and the secant method only at order
 * 1.618, so Newton wins on iteration count. But Newton needs three evaluations
 * per iteration once the derivative has to be approximated - the value itself
 * plus two more for the central difference - while the secant method needs one,
 * reusing the previous value. For a function as expensive as zeta, that ratio
 * decides the matter.</p>
 */
class RootFinderComparisonTest {

    private static final double SQRT_TWO = 1.4142135623730951;

    private static final Function<Complex, Complex> Z_SQUARED_MINUS_TWO =
            z -> z.multiply(z).subtract(Complex.ofReal(2.0));

    /** Wraps a function so its invocations can be counted. */
    private static final class CountingFunction implements Function<Complex, Complex> {
        private final Function<Complex, Complex> delegate;
        private int callCount;

        CountingFunction(Function<Complex, Complex> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Complex apply(Complex z) {
            callCount++;
            return delegate.apply(z);
        }

        int callCount() {
            return callCount;
        }
    }

    @Nested
    @DisplayName("Iterations versus evaluations")
    class CostComparison {

        @Test
        @DisplayName("Newton needs fewer iterations, because its order is higher")
        void newtonNeedsFewerIterations() {
            int newtonIterations = new NewtonRootFinder()
                    .findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0)).iterations();
            int secantIterations = new SecantRootFinder()
                    .findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0)).iterations();

            assertTrue(newtonIterations < secantIterations,
                    "Newton " + newtonIterations + " vs secant " + secantIterations);
        }

        @Test
        @DisplayName("But the secant method needs fewer evaluations, which is what costs")
        void secantNeedsFewerEvaluations() {
            CountingFunction newtonTarget = new CountingFunction(Z_SQUARED_MINUS_TWO);
            CountingFunction secantTarget = new CountingFunction(Z_SQUARED_MINUS_TWO);

            new NewtonRootFinder().findRoot(newtonTarget, Complex.ofReal(3.0));
            new SecantRootFinder().findRoot(secantTarget, Complex.ofReal(3.0));

            assertTrue(secantTarget.callCount() < newtonTarget.callCount(),
                    "secant used " + secantTarget.callCount()
                            + " evaluations, Newton used " + newtonTarget.callCount()
                            + "; for an expensive function this is the number that matters");
        }

        @Test
        @DisplayName("Newton spends about three evaluations per iteration")
        void newtonSpendsThreeEvaluationsPerIteration() {
            CountingFunction target = new CountingFunction(Z_SQUARED_MINUS_TWO);
            RootFindingResult result =
                    new NewtonRootFinder().findRoot(target, Complex.ofReal(3.0));

            double perIteration = (double) target.callCount() / result.iterations();

            assertTrue(perIteration >= 2.5 && perIteration <= 3.5,
                    "expected about 3 evaluations per iteration, got " + perIteration);
        }

        @Test
        @DisplayName("The secant method spends about one evaluation per iteration")
        void secantSpendsOneEvaluationPerIteration() {
            CountingFunction target = new CountingFunction(Z_SQUARED_MINUS_TWO);
            RootFindingResult result =
                    new SecantRootFinder().findRoot(target, Complex.ofReal(3.0));

            double perIteration = (double) target.callCount() / result.iterations();

            assertTrue(perIteration <= 2.0,
                    "expected about 1 evaluation per iteration, got " + perIteration);
        }
    }

    @Nested
    @DisplayName("Observed convergence order")
    class ConvergenceOrder {

        @Test
        @DisplayName("Newton roughly doubles the number of correct digits each step")
        void newtonConvergesQuadratically() {
            double bestOrder = bestObservedOrder(
                    new NewtonRootFinder().findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0)));

            assertTrue(bestOrder > 1.8 && bestOrder < 2.3,
                    "expected order near 2, observed " + bestOrder);
        }

        @Test
        @DisplayName("The secant method converges more slowly than Newton but faster than linearly")
        void secantConvergesSuperlinearly() {
            double bestOrder = bestObservedOrder(
                    new SecantRootFinder().findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0)));

            assertTrue(bestOrder > 1.3,
                    "expected superlinear convergence, observed " + bestOrder);
            assertTrue(bestOrder < 2.3,
                    "the secant method cannot beat Newton's order, observed " + bestOrder);
        }

        /**
         * Estimates the convergence order from consecutive errors, taking the
         * best triple. Early iterations have not settled into the asymptotic
         * regime and later ones are dominated by rounding, so a single triple
         * is unreliable while the best of them is indicative.
         */
        private double bestObservedOrder(RootFindingResult result) {
            double best = 0.0;

            for (int i = 2; i < result.iterates().size(); i++) {
                double previous = error(result, i - 2);
                double current = error(result, i - 1);
                double next = error(result, i);

                boolean usable = next > 1e-15 && current > 1e-15 && previous > 1e-15
                        && next < current && current < previous;

                if (usable) {
                    double order = Math.log(next / current) / Math.log(current / previous);
                    best = Math.max(best, order);
                }
            }

            return best;
        }

        private double error(RootFindingResult result, int index) {
            return Math.abs(result.iterates().get(index).real() - SQRT_TWO);
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("A tighter iteration cap is respected")
        void iterationCapIsRespected() {
            RootFindingResult result = new NewtonRootFinder(1e-14, 1e-15, 2)
                    .findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(100.0));

            assertEquals(RootFindingResult.Termination.MAX_ITERATIONS_REACHED,
                    result.termination());
            assertEquals(2, result.iterations());
        }

        @Test
        @DisplayName("A loose residual tolerance stops earlier")
        void looseToleranceStopsEarlier() {
            int loose = new NewtonRootFinder(1e-3, 1e-15, 100)
                    .findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0)).iterations();
            int tight = new NewtonRootFinder(1e-14, 1e-15, 100)
                    .findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0)).iterations();

            assertTrue(loose < tight, "loose " + loose + " vs tight " + tight);
        }

        @Test
        @DisplayName("Nonsensical configuration is rejected at construction")
        void rejectsBadConfiguration() {
            assertThrows(IllegalArgumentException.class,
                    () -> new NewtonRootFinder(-1e-14, 1e-15, 100));
            assertThrows(IllegalArgumentException.class,
                    () -> new NewtonRootFinder(1e-14, 1e-15, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> new SecantRootFinder(-1e-14, 1e-15, 100));
            assertThrows(IllegalArgumentException.class,
                    () -> new SecantRootFinder(1e-14, 1e-15, 0));
        }
    }

    @Nested
    @DisplayName("The numerical derivative")
    class NumericalDerivative {

        @Test
        @DisplayName("A real-direction difference works, because the function is analytic")
        void realDirectionDifferenceSuffices() {
            // z^2 has derivative 2z, so at 3 + 4i the derivative is 6 + 8i.
            Function<Complex, Complex> square = z -> z.multiply(z);
            Complex point = Complex.of(3.0, 4.0);
            double h = 1e-6;

            Complex forward = square.apply(point.add(Complex.ofReal(h)));
            Complex backward = square.apply(point.subtract(Complex.ofReal(h)));
            Complex derivative = forward.subtract(backward)
                    .divide(Complex.ofReal(2.0 * h));

            assertEquals(6.0, derivative.real(), 1e-8,
                    "stepping along the real axis still recovers the full complex derivative");
            assertEquals(8.0, derivative.imaginary(), 1e-8);
        }

        @Test
        @DisplayName("Newton stops when the derivative vanishes rather than dividing by it")
        void newtonStopsAtAStationaryPoint() {
            // z^2 + 1 has derivative 2z, which is exactly zero at the origin.
            Function<Complex, Complex> function = z -> z.multiply(z).add(Complex.ONE);

            RootFindingResult result =
                    new NewtonRootFinder().findRoot(function, Complex.ZERO);

            assertEquals(RootFindingResult.Termination.DERIVATIVE_VANISHED,
                    result.termination());
        }
    }
}
