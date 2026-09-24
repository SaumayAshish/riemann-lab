package com.riemannlab.validation;

import com.riemannlab.core.complex.Complex;
import java.util.Objects;

/**
 * One evaluator's answer at a point with a known exact value, next to how
 * much error that answer actually carries and how much error the evaluator
 * itself claimed it might carry.
 *
 * <p>The interesting question is not "how accurate was this?" - it's
 * "was the evaluator honest about how accurate it was?" An error bound
 * that is routinely too small is more dangerous than one that is
 * pessimistic, because code downstream trusts it.</p>
 */
public record PrecisionSample(double s, Complex computedValue, double exactValue, double claimedErrorBound) {

    public PrecisionSample {
        if (!Double.isFinite(s)) {
            throw new IllegalArgumentException("s must be finite, got " + s);
        }
        Objects.requireNonNull(computedValue, "computedValue must not be null");
        if (!Double.isFinite(exactValue)) {
            throw new IllegalArgumentException("exactValue must be finite, got " + exactValue);
        }
        if (!Double.isFinite(claimedErrorBound) || claimedErrorBound < 0.0) {
            throw new IllegalArgumentException(
                    "claimedErrorBound must be finite and non-negative, got " + claimedErrorBound);
        }
    }

    /** How far the computed value actually landed from the known exact value. */
    public double actualError() {
        return computedValue.subtract(Complex.ofReal(exactValue)).magnitude();
    }

    /**
     * Actual error divided by the claimed bound. A value at or below 1.0
     * means the bound held; above 1.0 means the evaluator understated its
     * own error.
     *
     * <p>When the claimed bound is exactly zero, a zero actual error gives
     * a ratio of 0.0 (nothing to complain about); any positive actual
     * error gives {@link Double#POSITIVE_INFINITY}, since dividing by a
     * zero bound is otherwise undefined and "infinitely over budget" is
     * the honest description of that outcome.</p>
     */
    public double errorRatio() {
        double error = actualError();
        if (claimedErrorBound == 0.0) {
            return error == 0.0 ? 0.0 : Double.POSITIVE_INFINITY;
        }
        return error / claimedErrorBound;
    }

    /** Whether the actual error stayed within what the evaluator claimed. */
    public boolean isWithinClaimedBound() {
        return actualError() <= claimedErrorBound;
    }
}