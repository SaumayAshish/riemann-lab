package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.complex.ComplexMath;

/**
 * The relationship {@code zeta(s) = eta(s) / (1 - 2^(1-s))}, and the domain
 * restrictions that come with it.
 *
 * <p>Package-private because it is shared plumbing between the evaluators
 * rather than part of the project's public surface. Both of them need exactly
 * these checks, and two copies of a validation rule is two places to forget to
 * update.</p>
 */
final class EtaIdentity {

    /** Below this magnitude the denominator is treated as zero. */
    private static final double DENOMINATOR_FLOOR = 1e-12;

    /** Distance from s = 1 within which a failure is reported as the pole itself. */
    private static final double POLE_PROXIMITY = 1e-9;

    private EtaIdentity() {
        throw new AssertionError("EtaIdentity is a utility class and must not be instantiated");
    }

    /**
     * Returns {@code 1 - 2^(1-s)} after confirming that {@code s} lies in a
     * region where the identity can be used.
     *
     * @param s the point of interest
     * @return the denominator of the identity at {@code s}
     * @throws IllegalArgumentException if {@code Re(s) <= 0}
     * @throws ArithmeticException      if {@code s} is a singularity
     */
    static Complex denominatorAt(Complex s) {
        requireInsideEtaDomain(s);

        Complex exponent = Complex.ONE.subtract(s);
        Complex twoToTheExponent = ComplexMath.pow(Complex.ofReal(2), exponent);
        Complex denominator = Complex.ONE.subtract(twoToTheExponent);

        if (denominator.magnitude() < DENOMINATOR_FLOOR) {
            throw new ArithmeticException(describeSingularity(s));
        }

        return denominator;
    }

    /**
     * Rejects arguments where the eta series does not converge.
     *
     * <p>Eta converges for {@code Re(s) > 0}. Reaching the trivial zeros at
     * {@code s = -2, -4, -6, ...} requires the functional equation, which this
     * project does not yet implement.</p>
     *
     * @param s the point to check
     * @throws IllegalArgumentException if {@code Re(s) <= 0}
     */
    static void requireInsideEtaDomain(Complex s) {
        if (s.real() <= 0.0) {
            throw new IllegalArgumentException(
                    "the eta series converges only for Re(s) > 0, but Re(s) = " + s.real()
                            + ". Evaluating further left requires the functional equation,"
                            + " which this project does not implement.");
        }
    }

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