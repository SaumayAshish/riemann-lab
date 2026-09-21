package com.riemannlab.core.numeric;

import com.riemannlab.core.complex.Complex;
import java.util.function.Function;

/**
 * An iterative method for solving {@code f(s) = 0} where {@code s} is complex.
 *
 * <p>Nothing here is specialised to any particular function. Implementations
 * are verified against polynomials whose roots are known in closed form, so a
 * failure in a later search can be attributed to the target function rather
 * than the arithmetic.</p>
 *
 * <p><strong>Contract.</strong> Every implementation must:</p>
 * <ul>
 *   <li>return the caller's guess as the first iterate and the final answer as
 *       the last, so the history is complete;</li>
 *   <li>report a {@code Termination} that its own result honours - never claim
 *       convergence it did not achieve;</li>
 *   <li>terminate. No input may cause an unbounded loop;</li>
 *   <li>never divide by a vanishing slope, but report it instead;</li>
 *   <li>be deterministic, and safe to use from multiple threads.</li>
 * </ul>
 *
 * <p>Implementations take a single starting point. A method that needs two, as
 * the secant method does, derives the second itself - which keeps the
 * interface uniform and keeps the algorithm's internal needs out of the
 * caller's way.</p>
 */
public interface ComplexRootFinder {

    /**
     * Searches for a root of {@code function} starting from {@code initialGuess}.
     *
     * <p>The iterate is free to move anywhere in the complex plane. It is not
     * constrained to any line or region, which matters when the location of the
     * root is the question being asked.</p>
     *
     * @param function     the function whose root is sought; must not be null
     * @param initialGuess where to start; must not be null
     * @return the outcome, converged or not, never null
     * @throws NullPointerException if either argument is null
     */
    RootFindingResult findRoot(Function<Complex, Complex> function, Complex initialGuess);

    /**
     * A short identification of this method and its configuration, for logs,
     * reports and test failure messages.
     *
     * @return the finder's name
     */
    String name();
}