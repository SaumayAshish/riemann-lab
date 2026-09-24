package com.riemannlab.validation;

import com.riemannlab.core.complex.Complex;
import java.util.Objects;

/**
 * One point where zeta(s) was computed two independent ways - directly by
 * the accelerated eta series, and by reflecting through the functional
 * equation to 1-s - along with how far apart the two answers landed.
 *
 * <p>Both routes are valid for {@code 0 < Re(s) < 1}, so any disagreement
 * between them is evidence of a bug in one path or the other; it is not
 * something either evaluator can catch by checking itself.</p>
 */
public record ZetaAgreementSample(Complex point, Complex directValue, Complex reflectedValue) {

    public ZetaAgreementSample {
        Objects.requireNonNull(point, "point must not be null");
        Objects.requireNonNull(directValue, "directValue must not be null");
        Objects.requireNonNull(reflectedValue, "reflectedValue must not be null");
    }

    /** How far apart the two computed values are, in absolute terms. */
    public double absoluteDifference() {
        return directValue.subtract(reflectedValue).magnitude();
    }

    /**
     * The absolute difference as a fraction of the direct value's size.
     *
     * <p>Undefined - returned as {@code NaN} - when the direct value is
     * exactly zero, since there is nothing to take a fraction of. That can
     * only happen at a non-trivial zero itself, which none of this
     * project's cross-validation sample points are chosen to land on.</p>
     */
    public double relativeDifference() {
        double referenceMagnitude = directValue.magnitude();
        if (referenceMagnitude == 0.0) {
            return Double.NaN;
        }
        return absoluteDifference() / referenceMagnitude;
    }
}