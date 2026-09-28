package com.riemannlab.analysis;

/**
 * One sample of the magnitude of zeta along the critical line.
 *
 * <p>{@code height} is the imaginary part {@code t} in {@code s = 1/2 + it};
 * {@code magnitude} is {@code |zeta(1/2 + it)|}. Kept as a named pair rather
 * than a bare {@code double[]} so a list of these reads as what it is -
 * points on a graph - rather than an anonymous array of numbers.</p>
 *
 * <p>This type carries no opinion about zeros. A small magnitude here is a
 * hint that a zero is nearby, nothing more; the only authority on where a
 * zero actually is remains {@code ZeroRefiner}.</p>
 *
 * @param height the imaginary part t of the sample point s = 1/2 + it
 * @param magnitude the value |zeta(1/2 + it)| at that point
 */
public record CriticalLineSample(double height, double magnitude) {

    /** Validates that both the height and the magnitude are finite, and that the magnitude is non-negative. */
    public CriticalLineSample {
        if (!Double.isFinite(height)) {
            throw new IllegalArgumentException("height must be finite, got " + height);
        }
        if (!(magnitude >= 0.0) || !Double.isFinite(magnitude)) {
            throw new IllegalArgumentException(
                    "magnitude must be finite and non-negative, got " + magnitude);
        }
    }
}