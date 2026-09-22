package com.riemannlab.core.special;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.complex.ComplexMath;
import java.util.Objects;

/**
 * The gamma function of a complex argument, by the Lanczos approximation.
 *
 * <p>Gamma extends the factorial to every complex number, offset by one:
 * {@code Gamma(n) = (n-1)!} on the positive integers. It satisfies
 * {@code Gamma(z+1) = z Gamma(z)} everywhere, which is both its defining
 * property and the reason it has poles: rearranged as
 * {@code Gamma(z) = Gamma(z+1) / z}, it divides by zero at {@code z = 0} and,
 * by induction, at every negative integer. It has poles nowhere else, and it
 * is never zero.</p>
 *
 * <p><strong>Why not the integral.</strong> The definition
 * {@code Gamma(z) = integral of t^(z-1) e^(-t) dt} only converges for
 * {@code Re(z) > 0} - exactly the half-plane we are trying to escape - and
 * evaluating it numerically to fifteen digits would cost hundreds of
 * operations per call for a function that will sit inside a root-finding
 * loop.</p>
 *
 * <p><strong>The Lanczos approximation</strong> is Stirling's formula times a
 * nine-term rational correction. It reaches about fifteen decimal digits for
 * {@code Re(z) >= 1/2}, which is the whole of its validity: arguments to the
 * left of that are reflected across {@code Re(z) = 1/2} with
 * {@code Gamma(z)Gamma(1-z) = pi / sin(pi z)} and evaluated on the right.</p>
 *
 * <p>The coefficients below are Lanczos's published values for {@code g = 7}
 * with nine terms. They are not derived here and not adjustable; changing any
 * one of them destroys the accuracy of all the others.</p>
 *
 * <p>Stateless and thread-safe.</p>
 */
public final class GammaFunction {

    /**
     * Lanczos coefficients for {@code g = 7}, nine terms. Published values.
     */
    private static final double[] LANCZOS_COEFFICIENTS = {
            0.99999999999980993,
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
    };

    /** The Lanczos parameter the coefficients above were computed for. */
    private static final double LANCZOS_G = 7.0;

    /** Left of this the series is not valid and the reflection formula is used. */
    private static final double REFLECTION_THRESHOLD = 0.5;

    private static final double SQRT_TWO_PI = 2.5066282746310002;

    private GammaFunction() {
        throw new AssertionError("GammaFunction is a utility class and must not be instantiated");
    }

    /**
     * Evaluates the gamma function.
     *
     * @param z the argument; must not be null
     * @return {@code Gamma(z)}
     * @throws ArithmeticException if {@code z} is zero or a negative integer,
     *                             where gamma has a pole
     */
    public static Complex gamma(Complex z) {
        Objects.requireNonNull(z, "z must not be null");

        if (isPole(z)) {
            throw new ArithmeticException(
                    "gamma has a pole at " + z
                            + "; it is undefined at zero and at every negative integer");
        }

        if (z.real() < REFLECTION_THRESHOLD) {
            return reflectFromTheRightHalfPlane(z);
        }

        return lanczos(z);
    }

    /**
     * A pole needs all three conditions. A negative integer with any imaginary
     * part at all is an ordinary point where gamma is merely large, not
     * undefined - which is why this tests the imaginary part exactly rather
     * than against a tolerance.
     */
    private static boolean isPole(Complex z) {
        return z.imaginary() == 0.0
                && z.real() <= 0.0
                && z.real() == Math.rint(z.real());
    }

    /**
     * Applies {@code Gamma(z) = pi / (sin(pi z) Gamma(1-z))}. Since
     * {@code Re(z) < 1/2}, the reflected argument {@code 1-z} has
     * {@code Re(1-z) > 1/2} and lands where the series is valid.
     */
    private static Complex reflectFromTheRightHalfPlane(Complex z) {
        Complex sine = ComplexMath.sin(z.multiply(Complex.ofReal(Math.PI)));
        Complex reflected = lanczos(Complex.ONE.subtract(z));

        return Complex.ofReal(Math.PI).divide(sine.multiply(reflected));
    }

    /** The approximation proper. Valid only for {@code Re(z) >= 1/2}. */
    private static Complex lanczos(Complex z) {
        Complex shifted = z.subtract(Complex.ONE);

        Complex series = Complex.ofReal(LANCZOS_COEFFICIENTS[0]);
        for (int i = 1; i < LANCZOS_COEFFICIENTS.length; i++) {
            series = series.add(Complex.ofReal(LANCZOS_COEFFICIENTS[i])
                    .divide(shifted.add(Complex.ofReal(i))));
        }

        Complex t = shifted.add(Complex.ofReal(LANCZOS_G + 0.5));
        Complex stirling = ComplexMath.pow(t, shifted.add(Complex.ofReal(0.5)))
                .multiply(ComplexMath.exp(t.multiply(Complex.ofReal(-1.0))));

        return stirling.multiply(series).multiply(Complex.ofReal(SQRT_TWO_PI));
    }
}