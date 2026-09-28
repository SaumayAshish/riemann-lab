package com.riemannlab.zeros;

import com.riemannlab.core.complex.Complex;
import java.util.Objects;

/**
 * A candidate zero after refinement, with the evidence for or against it.
 *
 * <p><strong>Position is reported, never judged.</strong> Distance from the
 * critical line appears here as {@link #deviationFromCriticalLine()}, a
 * measurement, and it plays no part in {@link #isConfirmed()}. A zero is
 * confirmed by its residual being smaller than the evaluator's own error, not
 * by sitting where the Riemann Hypothesis predicts. Treating a real part away
 * from one half as a failure would build the hypothesis into the code and make
 * every result that followed circular.</p>
 *
 * @param startingPoint       where the search began
 * @param location            where it ended
 * @param residualMagnitude   {@code |zeta|} at the final location
 * @param evaluatorErrorBound the evaluator's claimed error there
 * @param iterations          root-finder steps taken
 * @param zetaEvaluations     how many times zeta was evaluated
 * @param outcome             what the refinement concluded
 */
public record RefinedZero(
        Complex startingPoint,
        Complex location,
        double residualMagnitude,
        double evaluatorErrorBound,
        int iterations,
        int zetaEvaluations,
        Outcome outcome) {

    /** What a refinement concluded. */
    public enum Outcome {

        /** Converged, with a residual within the evaluator's own error. */
        CONFIRMED,

        /** The root finder did not converge from this starting point. */
        NOT_CONVERGED,

        /**
         * Converged, but to a point where {@code |zeta|} is larger than the
         * evaluator's error allows for a genuine zero. Something was found;
         * it is not a zero.
         */
        RESIDUAL_ABOVE_BOUND,

        /**
         * The search wandered to {@code Re(s) <= 0}, where the eta series does
         * not converge and the evaluator refuses to answer. A limitation of
         * this evaluator rather than of the mathematics: the functional
         * equation extends zeta there, and this project does not yet use it.
         */
        LEFT_DOMAIN
    }
    /** Validates that neither point is null, the outcome is non-null, both magnitudes are non-negative, and neither cost is negative. */
    public RefinedZero {
        Objects.requireNonNull(startingPoint, "startingPoint must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");

        if (!(residualMagnitude >= 0.0)) {
            throw new IllegalArgumentException(
                    "residualMagnitude must be non-negative and not NaN, was "
                            + residualMagnitude);
        }
        if (!(evaluatorErrorBound >= 0.0)) {
            throw new IllegalArgumentException(
                    "evaluatorErrorBound must be non-negative and not NaN, was "
                            + evaluatorErrorBound);
        }
        if (iterations < 0 || zetaEvaluations < 0) {
            throw new IllegalArgumentException(
                    "costs must be non-negative, were " + iterations
                            + " iterations and " + zetaEvaluations + " evaluations");
        }
    }

    /**
     * The imaginary part of the refined location, conventionally written t.
     *
     * @return the height
     */
    public double height() {
        return location.imaginary();
    }

    /**
     * The real part of the refined location, conventionally written sigma.
     *
     * @return the real part
     */
    public double realPart() {
        return location.real();
    }

    /**
     * How far the refined location sits from the critical line.
     *
     * <p>Reported as data. The Riemann Hypothesis predicts zero; this is the
     * measurement against that prediction, for this zero only.</p>
     *
     * @return {@code |Re(s) - 0.5|}
     */
    public double deviationFromCriticalLine() {
        return Math.abs(location.real() - 0.5);
    }

    /**
     * How far the search travelled from its starting point.
     *
     * @return the distance from start to finish
     */
    public double distanceMoved() {
        return location.subtract(startingPoint).magnitude();
    }

    /**
     * Whether this is a confirmed zero.
     *
     * <p>Determined solely by convergence and by the residual falling within
     * the evaluator's error. Position is not consulted.</p>
     *
     * @return true when the outcome is {@link Outcome#CONFIRMED}
     */
    public boolean isConfirmed() {
        return outcome == Outcome.CONFIRMED;
    }

    @Override
    public String toString() {
        return String.format("RefinedZero[s=%s, |zeta|=%.2e, dev=%.2e, %s]",
                location, residualMagnitude, deviationFromCriticalLine(), outcome);
    }
}