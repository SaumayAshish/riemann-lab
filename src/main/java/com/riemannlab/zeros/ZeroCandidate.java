package com.riemannlab.zeros;

/**
 * A region of the critical line that plausibly contains a zero, found by
 * scanning, together with an estimate of where inside that region the zero
 * actually lies.
 *
 * <p>A scan can only report the grid point where {@code |zeta|} was smallest.
 * The zero itself is almost never on the grid. What makes the gap bridgeable
 * is that the non-trivial zeros are <em>simple</em>, so near a zero
 * {@code zeta(0.5 + it)} behaves like {@code zeta'(s0) * i * (t - t0)} and its
 * magnitude grows linearly away from the zero:</p>
 *
 * <pre>
 *     |zeta|
 *        |  o                      o        &lt;- magnitudeBefore, magnitudeAfter
 *        |    \                  /
 *        |      \      o       /             &lt;- magnitude, at height
 *        |        \  /   \   /
 *        +----------X------X----------- t
 *                   ^
 *                the zero
 * </pre>
 *
 * <p>Writing the three sampled values under that model, with the zero a
 * distance {@code d} from the sample and the grid spacing {@code h}:</p>
 *
 * <pre>
 *     magnitude        = S * d
 *     nearer neighbour = S * (h - d)
 *     farther neighbour= S * (h + d)          where S = |zeta'|
 * </pre>
 *
 * <p>Adding the two neighbours eliminates {@code d} and gives the slope;
 * dividing the magnitude by that slope gives {@code d}. With a step of 0.25
 * this locates the first three zeros to within about 1e-3, an excellent
 * starting point for a root-finder.</p>
 *
 * <p>Subtracting the magnitude from the <em>larger</em> neighbour instead
 * gives {@code S * h}, so the relative drop is {@code h / (h + d)} - a
 * quantity that can never fall below one half for a genuine zero. That bound
 * is what {@link #looksLikeZero()} tests, and it is what separates a zero from
 * a mere ripple in the curve.</p>
 *
 * @param height              the sampled height where the local minimum occurred
 * @param magnitude           {@code |zeta(0.5 + i*height)|}
 * @param stepSize            the scan step, so the bracket is height +/- stepSize
 * @param magnitudeBefore     magnitude one step below
 * @param magnitudeAfter      magnitude one step above
 * @param evaluatorErrorBound the evaluator's claimed error at this point
 */
public record ZeroCandidate(
        double height,
        double magnitude,
        double stepSize,
        double magnitudeBefore,
        double magnitudeAfter,
        double evaluatorErrorBound) {

    /**
     * Minimum drop from the larger neighbour, as a fraction of that neighbour,
     * for a bracket to be consistent with containing a simple zero.
     *
     * <p>Under the linear model the drop is exactly {@code h / (h + d)} with
     * {@code 0 <= d <= h}, so it cannot fall below one half. The threshold sits
     * slightly under that to allow for curvature in the real function.</p>
     */
    private static final double MINIMUM_RELATIVE_DROP = 0.4;

    public ZeroCandidate {
        if (!(stepSize > 0.0)) {
            throw new IllegalArgumentException("stepSize must be positive, was " + stepSize);
        }
        if (!(magnitude >= 0.0) || !(magnitudeBefore >= 0.0) || !(magnitudeAfter >= 0.0)) {
            throw new IllegalArgumentException(
                    "magnitudes must be non-negative and not NaN: " + magnitudeBefore
                            + ", " + magnitude + ", " + magnitudeAfter);
        }
        if (!(evaluatorErrorBound >= 0.0)) {
            throw new IllegalArgumentException(
                    "evaluatorErrorBound must be non-negative, was " + evaluatorErrorBound);
        }
        if (magnitude > magnitudeBefore || magnitude > magnitudeAfter) {
            throw new IllegalArgumentException(
                    "a candidate must be a local minimum, but " + magnitude
                            + " is not below both " + magnitudeBefore + " and " + magnitudeAfter);
        }
    }

    /**
     * The lower end of the bracketing interval.
     *
     * @return {@code height - stepSize}
     */
    public double bracketLower() {
        return height - stepSize;
    }

    /**
     * The upper end of the bracketing interval.
     *
     * @return {@code height + stepSize}
     */
    public double bracketUpper() {
        return height + stepSize;
    }

    /**
     * The width of the bracketing interval.
     *
     * @return {@code 2 * stepSize}
     */
    public double bracketWidth() {
        return 2.0 * stepSize;
    }

    /**
     * Reports whether a given height falls inside this candidate's bracket.
     *
     * @param someHeight the height to test
     * @return true when the height lies within the bracket, inclusive
     */
    public boolean brackets(double someHeight) {
        return someHeight >= bracketLower() && someHeight <= bracketUpper();
    }

    /**
     * Estimates {@code |zeta'|} at the zero, from the two arms of the V.
     *
     * <p><strong>Only meaningful when this bracket really does contain a
     * zero.</strong> The derivation assumes the V passes through the axis; for
     * a shallow dip high above it the formula returns a large and meaningless
     * value. Check {@link #looksLikeZero()} before trusting this.</p>
     *
     * @return the estimated slope, zero when the neighbourhood is flat
     */
    public double estimatedSlope() {
        return (magnitudeBefore + magnitudeAfter) / (2.0 * stepSize);
    }

    /**
     * Estimates how far the zero lies from the sampled height.
     *
     * <p>Depth of the V divided by its slope, clamped to the bracket. A flat
     * neighbourhood yields zero rather than a division by zero: with no
     * gradient there is no evidence the zero is anywhere but here.</p>
     *
     * @return the estimated distance, never negative and never beyond the bracket
     */
    public double estimatedDistanceToZero() {
        double slope = estimatedSlope();

        if (!(slope > 0.0)) {
            return 0.0;
        }

        return Math.min(magnitude / slope, stepSize);
    }

    /**
     * Estimates the height of the zero itself, moving from the sampled
     * minimum toward whichever neighbour is smaller.
     *
     * @return the estimated zero height, always inside the bracket
     */
    public double estimatedHeight() {
        double distance = estimatedDistanceToZero();

        return magnitudeBefore <= magnitudeAfter
                ? height - distance
                : height + distance;
    }

    /**
     * The drop from the larger neighbour to this minimum, as a fraction of
     * that neighbour.
     *
     * <p>A deep V gives a value near 1; a shallow ripple on a curve that never
     * approaches the axis gives a value near 0. The dip in {@code |zeta|} near
     * t = 2.4, which is not a zero, drops by about 0.01.</p>
     *
     * @return the relative drop, between 0 and 1
     */
    public double relativeDrop() {
        double largerNeighbour = Math.max(magnitudeBefore, magnitudeAfter);

        if (!(largerNeighbour > 0.0)) {
            return 0.0;
        }

        return (largerNeighbour - magnitude) / largerNeighbour;
    }

    /**
     * Reports whether this bracket is consistent with containing a simple zero.
     *
     * <p>Tests the relative drop from the larger neighbour, which the linear
     * model bounds below by one half. A shallow local minimum sitting well
     * above the axis - and {@code |zeta|} on the critical line has one near
     * t = 2.4, with magnitude about 0.53 - drops by only a few percent and is
     * correctly rejected.</p>
     *
     * <p>This is a necessary condition, not a sufficient one. It filters
     * obvious non-zeros cheaply; confirming a zero is the root-finder's job.</p>
     *
     * @return true when the drop is steep enough for a zero to lie inside
     */
    public boolean looksLikeZero() {
        double largerNeighbour = Math.max(magnitudeBefore, magnitudeAfter);

        if (!(largerNeighbour > 0.0)) {
            return magnitude <= evaluatorErrorBound;
        }

        return relativeDrop() >= MINIMUM_RELATIVE_DROP;
    }

    @Override
    public String toString() {
        return String.format("ZeroCandidate[t~%.6f, |zeta|=%.3e, drop=%.3f, bracket=[%.4f, %.4f]]",
                estimatedHeight(), magnitude, relativeDrop(), bracketLower(), bracketUpper());
    }
}