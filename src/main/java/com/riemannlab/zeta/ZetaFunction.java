package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.complex.ComplexMath;

/**
 * Evaluates the Riemann zeta function through its relationship with the
 * Dirichlet eta function:
 *
 * <pre>
 *     zeta(s) = eta(s) / (1 - 2^(1-s))
 * </pre>
 *
 * <p>The identity is derived by subtracting the two series termwise, which is
 * valid where both converge, that is {@code Re(s) > 1}. It is used here
 * throughout {@code Re(s) > 0} on the strength of analytic continuation: the
 * right-hand side is well behaved across the critical strip and agrees with
 * zeta wherever zeta is directly defined, so it is zeta there.</p>
 *
 * <p><strong>Singularities of the identity.</strong> The denominator vanishes
 * when {@code 2^(1-s) = 1}, that is at {@code s = 1 + 2*pi*i*k/ln(2)} for
 * integer {@code k}. At {@code k = 0} this is {@code s = 1}, where zeta
 * genuinely has a simple pole. At every other {@code k} zeta is perfectly
 * regular and eta vanishes too, so the true value is a removable 0/0 limit
 * that this formula cannot evaluate. All such points lie on
 * {@code Re(s) = 1}. On the critical line the magnitude of {@code 2^(1-s)} is
 * {@code sqrt(2)}, so the denominator never falls below about 0.414 and this
 * failure mode cannot arise there.</p>
 *
 * <p><strong>This evaluator is deliberately simple.</strong> Its accuracy is
 * limited by the raw eta partial sum, which converges like
 * {@code N^-Re(s)}. It is correct, slow, and easy to check - the right thing
 * to have before adding acceleration.</p>
 */
public final class ZetaFunction {

    /**
     * Below this magnitude the denominator is treated as zero. On the critical
     * line the denominator never drops below roughly 0.414, so this threshold
     * only ever triggers near {@code Re(s) = 1}.
     */
    private static final double DENOMINATOR_FLOOR = 1e-12;

    /** Distance from s = 1 within which a failure is reported as the pole itself. */
    private static final double POLE_PROXIMITY = 1e-9;

    private ZetaFunction() {
        throw new AssertionError("ZetaFunction is a utility class and must not be instantiated");
    }

    /**
     * Evaluates zeta at {@code s} using {@code termCount} terms of the eta
     * series.
     *
     * <p>The denominator is checked before the series is summed. That check
     * costs one complex power; performing it afterwards would waste
     * {@code termCount} of them before failing.</p>
     *
     * @param s         the point at which to evaluate zeta
     * @param termCount how many eta terms to sum; must be at least 1
     * @return an approximation of {@code zeta(s)}
     * @throws IllegalArgumentException if {@code termCount} is less than 1
     * @throws ArithmeticException      if {@code s} is the pole at 1, or a
     *                                  removable singularity of this identity
     */
    public static Complex evaluate(Complex s, int termCount) {
        if (termCount < 1) {
            throw new IllegalArgumentException(
                    "termCount must be at least 1, but was " + termCount);
        }

        Complex denominator = etaToZetaDenominator(s);

        if (denominator.magnitude() < DENOMINATOR_FLOOR) {
            throw new ArithmeticException(describeSingularity(s));
        }

        return EtaFunction.partialSum(s, termCount).divide(denominator);
    }

    /**
     * Returns {@code 1 - 2^(1-s)}, the factor relating eta to zeta.
     *
     * @param s the point of interest
     * @return the denominator of the eta identity at {@code s}
     */
    private static Complex etaToZetaDenominator(Complex s) {
        Complex exponent = Complex.ONE.subtract(s);
        Complex twoToTheExponent = ComplexMath.pow(Complex.ofReal(2), exponent);
        return Complex.ONE.subtract(twoToTheExponent);
    }

    /**
     * Distinguishes the genuine pole of zeta from a removable singularity of
     * the identity, so the failure message says which one was hit.
     *
     * @param s the offending point
     * @return a message naming the actual cause
     */
    private static String describeSingularity(Complex s) {
        boolean isThePole = s.subtract(Complex.ONE).magnitude() < POLE_PROXIMITY;

        if (isThePole) {
            return "zeta has a simple pole at s = 1; no finite value exists there";
        }

        return "the eta identity has a removable singularity at s = " + s
                + " (a point where 2^(1-s) = 1). Zeta itself is regular here, but this"
                + " formula evaluates to 0/0 and cannot be used";
    }
}