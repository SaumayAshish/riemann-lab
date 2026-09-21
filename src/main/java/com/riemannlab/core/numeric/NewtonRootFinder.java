package com.riemannlab.core.numeric;

import com.riemannlab.core.complex.Complex;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Newton-Raphson root finding, with the derivative approximated by a central
 * difference.
 *
 * <p>Each step replaces the function by its tangent at the current iterate and
 * jumps to where that tangent crosses zero:</p>
 *
 * <pre>
 *     s(n+1) = s(n) - f(s(n)) / f'(s(n))
 * </pre>
 *
 * <p>Convergence is quadratic near a simple root, so the number of correct
 * digits roughly doubles per step. That is the headline figure and it is also
 * misleading: with no analytic derivative available, each step costs
 * <strong>three</strong> evaluations of {@code f} rather than one. Measured in
 * evaluations rather than iterations, {@link SecantRootFinder} is the cheaper
 * method - see {@code RootFinderComparisonTest}.</p>
 *
 * <p><strong>The derivative.</strong> A complex-differentiable function has the
 * same derivative in every direction, which is exactly what the Cauchy-Riemann
 * equations say. So a step along the real axis recovers the full complex
 * derivative and no direction-choosing is required. The step size balances
 * truncation error, which grows like {@code h^2}, against rounding, which grows
 * like {@code 1/h}; the optimum is near the cube root of the machine epsilon,
 * about 6e-6.</p>
 *
 * <p>Immutable and thread-safe.</p>
 */
public final class NewtonRootFinder implements ComplexRootFinder {

    private static final double DEFAULT_RESIDUAL_TOLERANCE = 1e-14;
    private static final double DEFAULT_STEP_TOLERANCE = 1e-15;
    private static final int DEFAULT_MAX_ITERATIONS = 100;

    /** Central-difference step, near the cube root of the machine epsilon. */
    private static final double DERIVATIVE_STEP = 1e-6;

    /** Below this the derivative is treated as vanished rather than divided by. */
    private static final double MINIMUM_USABLE_DERIVATIVE = 1e-300;

    /** Beyond this magnitude the iterate is considered to have run away. */
    private static final double DIVERGENCE_BOUND = 1e12;

    private final double residualTolerance;
    private final double stepTolerance;
    private final int maxIterations;

    /** Creates a finder with tolerances suitable for machine-precision work. */
    public NewtonRootFinder() {
        this(DEFAULT_RESIDUAL_TOLERANCE, DEFAULT_STEP_TOLERANCE, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Creates a finder with explicit stopping criteria.
     *
     * @param residualTolerance stop when {@code |f|} falls to this
     * @param stepTolerance     stop when the step falls to this
     * @param maxIterations     give up after this many steps
     * @throws IllegalArgumentException if any value is not positive
     */
    public NewtonRootFinder(double residualTolerance, double stepTolerance, int maxIterations) {
        if (!(residualTolerance > 0.0)) {
            throw new IllegalArgumentException(
                    "residualTolerance must be positive, was " + residualTolerance);
        }
        if (!(stepTolerance > 0.0)) {
            throw new IllegalArgumentException(
                    "stepTolerance must be positive, was " + stepTolerance);
        }
        if (maxIterations < 1) {
            throw new IllegalArgumentException(
                    "maxIterations must be at least 1, was " + maxIterations);
        }

        this.residualTolerance = residualTolerance;
        this.stepTolerance = stepTolerance;
        this.maxIterations = maxIterations;
    }

    @Override
    public RootFindingResult findRoot(
            Function<Complex, Complex> function, Complex initialGuess) {

        Objects.requireNonNull(function, "function must not be null");
        Objects.requireNonNull(initialGuess, "initialGuess must not be null");

        List<Complex> iterates = new ArrayList<>();
        iterates.add(initialGuess);

        Complex current = initialGuess;
        Complex value = function.apply(current);

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            if (value.magnitude() <= residualTolerance) {
                return new RootFindingResult(iterates, value.magnitude(),
                        RootFindingResult.Termination.RESIDUAL_BELOW_TOLERANCE);
            }

            Complex derivative = approximateDerivative(function, current);

            if (derivative.magnitude() <= MINIMUM_USABLE_DERIVATIVE) {
                return new RootFindingResult(iterates, value.magnitude(),
                        RootFindingResult.Termination.DERIVATIVE_VANISHED);
            }

            Complex step = value.divide(derivative);
            current = current.subtract(step);
            iterates.add(current);

            if (!isFinite(current) || current.magnitude() > DIVERGENCE_BOUND) {
                return new RootFindingResult(iterates, value.magnitude(),
                        RootFindingResult.Termination.DIVERGED);
            }

            value = function.apply(current);

            if (step.magnitude() <= stepTolerance) {
                return new RootFindingResult(iterates, value.magnitude(),
                        RootFindingResult.Termination.STEP_BELOW_TOLERANCE);
            }
        }

        return new RootFindingResult(iterates, value.magnitude(),
                RootFindingResult.Termination.MAX_ITERATIONS_REACHED);
    }

    @Override
    public String name() {
        return String.format("newton(res=%.0e, step=%.0e, max=%d)",
                residualTolerance, stepTolerance, maxIterations);
    }

    /**
     * Central difference along the real axis. Valid in any direction because
     * the function is assumed analytic.
     */
    private Complex approximateDerivative(
            Function<Complex, Complex> function, Complex point) {

        Complex offset = Complex.ofReal(DERIVATIVE_STEP);
        Complex forward = function.apply(point.add(offset));
        Complex backward = function.apply(point.subtract(offset));

        return forward.subtract(backward).divide(Complex.ofReal(2.0 * DERIVATIVE_STEP));
    }

    private static boolean isFinite(Complex z) {
        return Double.isFinite(z.real()) && Double.isFinite(z.imaginary());
    }
}