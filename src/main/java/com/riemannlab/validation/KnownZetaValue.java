package com.riemannlab.validation;

/**
 * One point where the exact value of zeta(s) is known in closed form,
 * paired with that exact value - ground truth for checking whether an
 * evaluator's self-reported error bound can be trusted.
 *
 * @param s the real point at which zeta's exact value is known
 * @param exactValue the exact value of zeta(s) at that point
 */
public record KnownZetaValue(double s, double exactValue) {

    /** Validates that both {@code s} and {@code exactValue} are finite. */
    public KnownZetaValue {
        if (!Double.isFinite(s)) {
            throw new IllegalArgumentException("s must be finite, got " + s);
        }
        if (!Double.isFinite(exactValue)) {
            throw new IllegalArgumentException("exactValue must be finite, got " + exactValue);
        }
    }
}