package com.riemannlab.core.numeric;

import com.riemannlab.core.complex.Complex;
import java.util.function.IntFunction;

/**
 * Sums an alternating series using the Cohen-Rodriguez Villegas-Zagier
 * acceleration algorithm (Experimental Mathematics, 2000), closely related to
 * Borwein's 1995 algorithm for the zeta function.
 *
 * <p>Given terms {@code a_0, a_1, a_2, ...} the algorithm approximates
 * {@code a_0 - a_1 + a_2 - a_3 + ...} without summing the series naively.
 * Instead of weights of alternating sign and unit size it uses weights derived
 * from Chebyshev polynomials, which extract the limit from the pattern of
 * oscillation rather than waiting for the terms to die away.</p>
 *
 * <p><strong>Convergence.</strong> The error shrinks by a factor of
 * {@code 3 + sqrt(8) = 5.8284...} for every additional term - about 0.766
 * decimal digits each. Twenty-five terms reach the limit of {@code double}
 * precision, where the naive alternating sum would need roughly 1e15.</p>
 *
 * <p><strong>Applicability.</strong> The bound above assumes well-behaved
 * terms. For complex arguments whose terms rotate, the bound degrades by
 * roughly {@code e^(pi*|Im|/2)}, so callers evaluating oscillatory series must
 * raise the order accordingly. This class does not attempt to choose an order
 * for the caller; it cannot know what the terms represent.</p>
 */
public final class AlternatingSeriesAccelerator {

    /**
     * The per-term error reduction factor, {@code 3 + sqrt(8) = 5.8284...}.
     * It arises because the algorithm's normaliser is the Chebyshev polynomial
     * {@code T_n(3)}, which grows like this base.
     */
    private static final double CONVERGENCE_BASE = 3.0 + Math.sqrt(8.0);

    /**
     * Highest order this implementation accepts. The normaliser grows like
     * {@code CONVERGENCE_BASE^order}, which at 250 is around 1e191 - large,
     * but still comfortably inside the range of {@code double}.
     */
    public static final int MAX_ORDER = 250;

    private AlternatingSeriesAccelerator() {
        throw new AssertionError(
                "AlternatingSeriesAccelerator is a utility class and must not be instantiated");
    }

    /**
     * Approximates {@code a_0 - a_1 + a_2 - a_3 + ...} using {@code order}
     * terms.
     *
     * <p>The caller supplies the terms <em>without</em> signs: the alternation
     * is part of the algorithm, not part of the input.</p>
     *
     * @param terms supplies {@code a_k} for {@code k = 0, 1, ..., order - 1}
     * @param order how many terms to use; each one cuts the error by about 5.83
     * @return the accelerated sum
     * @throws IllegalArgumentException if {@code order} is below 1 or above
     *                                  {@link #MAX_ORDER}
     */
    public static Complex sum(IntFunction<Complex> terms, int order) {
        if (order < 1) {
            throw new IllegalArgumentException("order must be at least 1, but was " + order);
        }
        if (order > MAX_ORDER) {
            throw new IllegalArgumentException(
                    "order must be at most " + MAX_ORDER + ", but was " + order
                            + "; beyond that the normaliser approaches the limits of double precision");
        }

        double normaliser = Math.pow(CONVERGENCE_BASE, order);
        normaliser = (normaliser + 1.0 / normaliser) / 2.0;

        double b = -1.0;
        double c = -normaliser;
        Complex weightedSum = Complex.ZERO;

        for (int k = 0; k < order; k++) {
            c = b - c;
            weightedSum = weightedSum.add(terms.apply(k).multiply(Complex.ofReal(c)));
            b = (k + order) * (double) (k - order) * b / ((k + 0.5) * (k + 1.0));
        }

        // Scale by the real normaliser component-wise rather than calling
        // Complex.divide. That method forms c^2 + d^2 internally, and at high
        // orders the normaliser exceeds 1e154, so squaring it overflows to
        // infinity and would silently return zero.
        return Complex.of(
                weightedSum.real() / normaliser,
                weightedSum.imaginary() / normaliser);
    }

    /**
     * Returns the theoretical error bound for well-behaved terms at the given
     * order, {@code (3 + sqrt(8))^-order}.
     *
     * <p>This is the bound for real, smoothly decreasing terms. Oscillatory
     * complex terms degrade it, so it is a guide for choosing an order rather
     * than a guarantee.</p>
     *
     * @param order the acceleration order
     * @return the error bound
     */
    public static double errorBound(int order) {
        return Math.pow(CONVERGENCE_BASE, -order);
    }
}
