package com.riemannlab.core.numeric;

import com.riemannlab.core.complex.Complex;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Secant root finding: Newton's method with the tangent replaced by the chord
 * through the two most recent iterates.
 *
 * <pre>
 *     s(n+1) = s(n) - f(s(n)) * (s(n) - s(n-1)) / (f(s(n)) - f(s(n-1)))
 * </pre>
 *
 * <p>No derivative is required, which is the point: the zeta function has no
 * derivative available in closed form here, and approximating one costs two
 * extra evaluations per step.</p>
 *
 * <p><strong>Convergence.</strong> The order is the golden ratio,
 * {@code (1 + sqrt(5)) / 2 = 1.618...}, lower than Newton's 2. The error
 * recurrence is {@code e(n+1) ~ C * e(n) * e(n-1)}, whose exponents are the
 * Fibonacci numbers - which is why the golden ratio appears.</p>
 *
 * <p>But each step needs only <strong>one</strong> new evaluation, reusing the
 * previous value, against Newton's three. Per evaluation this method gains
 * about {@code log10(1.618) = 0.21} decimal digits against Newton's
 * {@code log10(2)/3 = 0.10}. For an expensive target it is roughly twice as
 * efficient, and that is the measure that matters.</p>
 *
 * <p>The interface supplies one starting point, so the second is derived by a
 * small offset. That keeps callers from having to know which methods need two
 * points.</p>
 *
 * <p>Immutable and thread-safe.</p>
 */
public final class SecantRootFinder implements ComplexRootFinder {

    private static final double DEFAULT_RESIDUAL_TOLERANCE = 1e-14;
    private static final double DEFAULT_STEP_TOLERANCE = 1e-15;
    private static final int DEFAULT_MAX_ITERATIONS = 100;

    /**
     * Offset used to manufacture the second starting point. Small enough that
     * the chord approximates the tangent, large enough that the difference of
     * the two function values is not lost to rounding.
     */
    private static final double SECOND_POINT_OFFSET = 1e-4;

    /** Below this the chord slope is treated as vanished rather than divided by. */
    private static final double MINIMUM_USABLE_SLOPE = 1e-300;

    /** Beyond this magnitude the iterate is considered to have run away. */
    private static final double DIVERGENCE_BOUND = 1e12;

    private final double residualTolerance;
    private final double stepTolerance;
    private final int maxIterations;

    /** Creates a finder with tolerances suitable for machine-precision work. */
    public SecantRootFinder() {
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
    public SecantRootFinder(double residualTolerance, double stepTolerance, int maxIterations) {
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

        Complex previous = initialGuess;
        Complex previousValue = function.apply(previous);

        if (previousValue.magnitude() <= residualTolerance) {
            return new RootFindingResult(iterates, previousValue.magnitude(),
                    RootFindingResult.Termination.RESIDUAL_BELOW_TOLERANCE);
        }

        Complex current = initialGuess.add(Complex.ofReal(SECOND_POINT_OFFSET));
        iterates.add(current);

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            Complex value = function.apply(current);

            if (value.magnitude() <= residualTolerance) {
                return new RootFindingResult(iterates, value.magnitude(),
                        RootFindingResult.Termination.RESIDUAL_BELOW_TOLERANCE);
            }

            Complex valueChange = value.subtract(previousValue);

            if (valueChange.magnitude() <= MINIMUM_USABLE_SLOPE) {
                return new RootFindingResult(iterates, value.magnitude(),
                        RootFindingResult.Termination.DERIVATIVE_VANISHED);
            }

            Complex step = value
                    .multiply(current.subtract(previous))
                    .divide(valueChange);

            previous = current;
            previousValue = value;
            current = current.subtract(step);
            iterates.add(current);

            if (!isFinite(current) || current.magnitude() > DIVERGENCE_BOUND) {
                return new RootFindingResult(iterates, value.magnitude(),
                        RootFindingResult.Termination.DIVERGED);
            }

            // The movement that actually happened, not the movement requested.
            // Near a root at height t a requested step smaller than ulp(t) is
            // absorbed by the subtraction above and the iterate does not move
            // at all. Testing the requested step misses that, and the next
            // pass then compares the point with itself: the two function
            // values are bit-identical, the chord slope is exactly zero, and a
            // converged search is reported as a vanished derivative.
            if (current.subtract(previous).magnitude() <= stepTolerance) {
                return new RootFindingResult(iterates,
                        function.apply(current).magnitude(),
                        RootFindingResult.Termination.STEP_BELOW_TOLERANCE);
            }
        }

        return new RootFindingResult(iterates, function.apply(current).magnitude(),
                RootFindingResult.Termination.MAX_ITERATIONS_REACHED);
    }

    @Override
    public String name() {
        return String.format("secant(res=%.0e, step=%.0e, max=%d)",
                residualTolerance, stepTolerance, maxIterations);
    }

    private static boolean isFinite(Complex z) {
        return Double.isFinite(z.real()) && Double.isFinite(z.imaginary());
    }
}