package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;

/**
 * The outcome of one zeta evaluation, together with enough information to
 * judge how far it can be trusted.
 *
 * <p>A bare {@link Complex} would not be enough. A numerical zero cannot be
 * recognised by comparing against zero - every evaluation carries error, so
 * the only honest question is whether the computed magnitude is smaller than
 * the error of the computation that produced it. That question needs the
 * error bound travelling alongside the value, which is what this record
 * provides.</p>
 *
 * @param value               the computed value of zeta
 * @param termsUsed           how many series terms were evaluated
 * @param estimatedErrorBound an upper estimate of the absolute error in
 *                            {@code value}; an estimate, not a guarantee
 */
public record ZetaResult(Complex value, int termsUsed, double estimatedErrorBound) {
    /** Validates that at least one term was used and the error bound is non-negative. */
    public ZetaResult {
        if (termsUsed < 1) {
            throw new IllegalArgumentException("termsUsed must be at least 1, was " + termsUsed);
        }
        if (!(estimatedErrorBound >= 0.0)) {
            throw new IllegalArgumentException(
                    "estimatedErrorBound must be non-negative and not NaN, was "
                            + estimatedErrorBound);
        }
    }

    /**
     * Reports whether this value is too small to be told apart from zero at
     * the precision that produced it.
     *
     * <p>This is the correct numerical test for a zero, and the reason
     * {@code value.equals(Complex.ZERO)} is meaningless here: a true zero of
     * zeta almost never computes to exactly 0.0, and a value that does compute
     * to exactly 0.0 is not thereby a zero.</p>
     *
     * @return true when the magnitude does not exceed the error bound
     */
    public boolean isIndistinguishableFromZero() {
        return value.magnitude() <= estimatedErrorBound;
    }
}