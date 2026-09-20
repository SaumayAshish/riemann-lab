package com.riemannlab.core.complex;

/**
 * Transcendental functions over {@link Complex}: the exponential, the
 * principal logarithm, and general complex exponentiation.
 *
 * <p>These three functions are what give meaning to {@code n^s} when the
 * exponent {@code s} is complex. Repeated multiplication is undefined for a
 * complex exponent, so the power is defined as
 * {@code z^w = exp(w * log(z))} - which makes every term of the Riemann zeta
 * series computable.</p>
 *
 * <p>This is a stateless utility class, deliberately kept separate from
 * {@link Complex}. {@code Complex} is a pure value type carrying only
 * arithmetic; these functions carry precision and branch-cut policy, which is
 * a different concern. The split mirrors {@code java.lang.Math} standing
 * apart from {@code java.lang.Double}.</p>
 */
public final class ComplexMath {

    private ComplexMath() {
        throw new AssertionError("ComplexMath is a utility class and must not be instantiated");
    }

    /**
     * Returns {@code e} raised to the power of {@code z}.
     *
     * <p>Splitting the exponent gives
     * {@code e^(a + bi) = e^a * (cos b + i sin b)}, so the real part of the
     * input sets the magnitude of the result and the imaginary part sets its
     * angle. A purely imaginary exponent therefore produces a point on the
     * unit circle - pure rotation, no change in size. That rotation is the
     * source of the oscillation in the zeta function.</p>
     *
     * <p><strong>Numerical caveat:</strong> the magnitude {@code e^a}
     * overflows to infinity once the real part exceeds about 709.78, and
     * underflows to zero below about -745. Zeta evaluations in this project
     * stay far inside that range.</p>
     *
     * @param z the exponent; must not be null
     * @return {@code e^z}
     */
    public static Complex exp(Complex z) {
        double magnitude = Math.exp(z.real());
        return new Complex(
                magnitude * Math.cos(z.imaginary()),
                magnitude * Math.sin(z.imaginary()));
    }

    /**
     * Returns the principal complex logarithm of {@code z}.
     *
     * <p>{@code log(z) = ln|z| + i * arg(z)}, where {@code arg(z)} is the
     * angle from the positive real axis.</p>
     *
     * <p><strong>Branch cut.</strong> An angle is only defined up to multiples
     * of {@code 2*pi}, so the complex logarithm has infinitely many values.
     * This method returns the principal one, with the angle in
     * {@code (-pi, pi]}. The function is consequently discontinuous across the
     * negative real axis: just above it the angle is near {@code +pi}, just
     * below it near {@code -pi}. Callers that need a continuous logarithm
     * along a path crossing that axis must track the winding themselves.</p>
     *
     * @param z the argument; must not be null
     * @return the principal logarithm
     * @throws ArithmeticException if {@code z} is exactly zero
     */
    public static Complex log(Complex z) {
        double magnitude = z.magnitude();

        if (magnitude == 0.0) {
            throw new ArithmeticException("The complex logarithm is undefined at zero");
        }

        return new Complex(
                Math.log(magnitude),
                Math.atan2(z.imaginary(), z.real()));
    }

    /**
     * Raises {@code base} to the power {@code exponent}, both complex.
     *
     * <p>Defined as {@code exp(exponent * log(base))}. Validation of the base
     * is delegated to {@link #log}, which is the method that actually has the
     * domain restriction - duplicating the check here would let the two drift
     * apart.</p>
     *
     * <p>Because it is built on the principal logarithm, this method returns
     * the principal value of the power and inherits the branch cut described
     * in {@link #log}. For the positive real bases used by the zeta series
     * the angle is zero, so no branch ambiguity arises there.</p>
     *
     * @param base     the base; must not be null
     * @param exponent the exponent; must not be null
     * @return {@code base^exponent}, principal value
     * @throws ArithmeticException if {@code base} is exactly zero
     */
    public static Complex pow(Complex base, Complex exponent) {
        return exp(exponent.multiply(log(base)));
    }
}