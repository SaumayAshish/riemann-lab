package com.riemannlab.core.numeric;

import com.riemannlab.core.complex.Complex;
import java.util.List;
import java.util.Objects;

/**
 * The outcome of a root search: where it ended up, what it cost, and why it
 * stopped.
 *
 * <p>The full iterate history is retained rather than just the final answer.
 * It costs a handful of objects, it is what makes the convergence order
 * measurable instead of assumed, and a later phase will draw it.</p>
 *
 * @param iterates          every point visited, starting with the caller's
 *                          guess and ending with the final answer
 * @param residualMagnitude {@code |f|} at the final iterate
 * @param termination       why the search stopped
 */
public record RootFindingResult(
        List<Complex> iterates,
        double residualMagnitude,
        Termination termination) {

    /**
     * Why a root search stopped.
     *
     * <p>Two of these are success and they are not the same success. A small
     * residual says the function is nearly zero at that point. A small step
     * says further iterations cannot improve the answer. A steep function can
     * stop moving while its residual is still visible; a flat one can have a
     * tiny residual some way from the root.</p>
     */
    public enum Termination {

        /** {@code |f|} fell below the residual tolerance. */
        RESIDUAL_BELOW_TOLERANCE,

        /** The iterate stopped moving, so no further progress is possible. */
        STEP_BELOW_TOLERANCE,

        /** The iteration cap was reached without either criterion being met. */
        MAX_ITERATIONS_REACHED,

        /**
         * The slope used to take the next step was effectively zero, so the
         * step could not be computed. Reported rather than divided by.
         */
        DERIVATIVE_VANISHED,

        /** The iterate ran away or became non-finite. */
        DIVERGED;

        /**
         * Whether this termination means a root was found.
         *
         * @return true for the two convergence criteria, false otherwise
         */
        public boolean isSuccess() {
            return this == RESIDUAL_BELOW_TOLERANCE || this == STEP_BELOW_TOLERANCE;
        }
    }

    public RootFindingResult {
        Objects.requireNonNull(iterates, "iterates must not be null");
        Objects.requireNonNull(termination, "termination must not be null");

        if (iterates.isEmpty()) {
            throw new IllegalArgumentException(
                    "iterates must contain at least the starting point");
        }
        if (!(residualMagnitude >= 0.0)) {
            throw new IllegalArgumentException(
                    "residualMagnitude must be non-negative and not NaN, was "
                            + residualMagnitude);
        }

        iterates = List.copyOf(iterates);
    }

    /**
     * The final iterate.
     *
     * @return the point the search ended on, root or not
     */
    public Complex root() {
        return iterates.get(iterates.size() - 1);
    }

    /**
     * How many steps were taken, which is one fewer than the number of points
     * visited.
     *
     * @return the iteration count
     */
    public int iterations() {
        return iterates.size() - 1;
    }

    /**
     * Whether a root was found.
     *
     * @return true when the termination reason is one of the success cases
     */
    public boolean converged() {
        return termination.isSuccess();
    }

    @Override
    public String toString() {
        return String.format("RootFindingResult[root=%s, iterations=%d, residual=%.3e, %s]",
                root(), iterations(), residualMagnitude, termination);
    }
}