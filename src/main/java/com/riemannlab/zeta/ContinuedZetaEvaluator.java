package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.complex.ComplexMath;
import com.riemannlab.core.special.GammaFunction;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates zeta on the whole complex plane, by analytic continuation through
 * the functional equation.
 *
 * <pre>
 *     zeta(s) = 2^s * pi^(s-1) * sin(pi s / 2) * Gamma(1-s) * zeta(1-s)
 * </pre>
 *
 * <p>To the right of {@code Re(s) = 1/2} the work is delegated to a base
 * evaluator, which is where the eta series is at its best. To the left, the
 * equation above is used: the reflected point {@code 1-s} then has
 * {@code Re(1-s) > 1/2}, so the base evaluator handles it. The critical line
 * itself is delegated, which keeps the zero search of earlier phases running
 * on exactly the code it was validated against.</p>
 *
 * <p><strong>The trivial zeros.</strong> At {@code s = -2, -4, -6, ...} the
 * sine factor vanishes while every other factor is finite and non-zero, so
 * zeta is zero there. Nothing in this class knows that; it falls out of the
 * arithmetic, and the computed values come back indistinguishable from zero at
 * the precision available.</p>
 *
 * <p><strong>The symmetry.</strong> Strictly inside the critical strip every
 * factor except {@code zeta(1-s)} is non-zero - the exponentials never vanish,
 * the sine has no even integer to vanish at, and gamma is never zero anywhere.
 * So {@code zeta(s) = 0} exactly when {@code zeta(1-s) = 0}: the non-trivial
 * zeros are forced into mirror pairs about the critical line. Together with
 * conjugate symmetry that makes a quadruple, which collapses to a pair only
 * when the zero lies on the line itself. This class measures that; it does not
 * and cannot establish that the collapse always happens.</p>
 *
 * <p>Immutable and thread-safe, given a thread-safe base evaluator.</p>
 */
public final class ContinuedZetaEvaluator implements ZetaEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ContinuedZetaEvaluator.class);

    /** Left of this the functional equation is used; at or right of it, the base. */
    private static final double REFLECTION_THRESHOLD = 0.5;

    /**
     * {@code zeta(0) = -1/2}. The functional equation is indeterminate here -
     * the sine is zero and {@code zeta(1-s)} is the pole at {@code s = 1}, so
     * the product reads {@code 0 * infinity}. The value is supplied rather
     * than computed, exactly as a removable singularity requires.
     */
    private static final Complex ZETA_AT_ZERO = Complex.ofReal(-0.5);

    /**
     * Below this real part {@code Gamma(1-s)} overflows a double -
     * {@code Gamma(172)} is already infinite - and the product would be NaN.
     */
    private static final double MINIMUM_SUPPORTED_REAL_PART = -170.0;

    /**
     * Multiple of the machine epsilon used for the gamma approximation's
     * relative error, scaled by height. Chosen empirically against the exact
     * identity {@code |Gamma(1/2+it)| = sqrt(pi / cosh(pi t))}, with margin.
     */
    private static final double GAMMA_ROUNDOFF_TERMS = 32.0;

    private static final double LN_TWO = Math.log(2.0);
    private static final double LN_PI = Math.log(Math.PI);

    private final ZetaEvaluator base;

    /** Creates an evaluator over the default accelerated eta evaluator. */
    public ContinuedZetaEvaluator() {
        this(new AcceleratedEtaEvaluator());
    }

    /**
     * Creates an evaluator over an explicit base.
     *
     * @param base the evaluator used at {@code Re(s) >= 1/2}; must not be null
     */
    public ContinuedZetaEvaluator(ZetaEvaluator base) {
        this.base = Objects.requireNonNull(base, "base evaluator must not be null");

        log.info("Created {}", name());
    }

    @Override
    public ZetaResult evaluate(Complex s) {
        Objects.requireNonNull(s, "s must not be null");

        if (s.real() >= REFLECTION_THRESHOLD) {
            return base.evaluate(s);
        }
        if (s.real() == 0.0 && s.imaginary() == 0.0) {
            return new ZetaResult(ZETA_AT_ZERO, 1, Math.ulp(0.5));
        }
        if (s.real() < MINIMUM_SUPPORTED_REAL_PART) {
            throw new IllegalArgumentException(
                    "Re(s) = " + s.real() + " is below " + MINIMUM_SUPPORTED_REAL_PART
                            + ", where Gamma(1-s) overflows a double. Evaluating further"
                            + " left requires extended-precision arithmetic, which this"
                            + " project does not implement.");
        }

        return continueLeftward(s);
    }

    @Override
    public String name() {
        return "continued(" + base.name() + ")";
    }

    private ZetaResult continueLeftward(Complex s) {
        Complex reflected = Complex.ONE.subtract(s);
        ZetaResult reflectedZeta = base.evaluate(reflected);

        Complex twoToTheS = ComplexMath.pow(Complex.ofReal(2.0), s);
        Complex piToTheSMinusOne =
                ComplexMath.pow(Complex.ofReal(Math.PI), s.subtract(Complex.ONE));
        Complex sine = ComplexMath.sin(s.multiply(Complex.ofReal(Math.PI / 2.0)));
        Complex gammaOfReflected = GammaFunction.gamma(reflected);

        Complex value = twoToTheS
                .multiply(piToTheSMinusOne)
                .multiply(sine)
                .multiply(gammaOfReflected)
                .multiply(reflectedZeta.value());

        if (!isFinite(value)) {
            throw new ArithmeticException(
                    "the functional equation overflowed at " + s
                            + "; the factors were 2^s = " + twoToTheS
                            + ", pi^(s-1) = " + piToTheSMinusOne
                            + ", Gamma(1-s) = " + gammaOfReflected);
        }

        double exponentials = twoToTheS.magnitude() * piToTheSMinusOne.magnitude();
        double withoutReflectedZeta =
                exponentials * sine.magnitude() * gammaOfReflected.magnitude();
        double withoutSine =
                exponentials * gammaOfReflected.magnitude() * reflectedZeta.value().magnitude();

        double bound = errorBoundAt(
                s, value.magnitude(), withoutReflectedZeta, withoutSine, reflectedZeta);

        return new ZetaResult(value, reflectedZeta.termsUsed(), bound);
    }

    /**
     * Propagates error through the five factors.
     *
     * <p>Relative errors add across a product, so the gamma approximation and
     * the two complex powers contribute proportionally to the result. The base
     * evaluator's error is taken absolutely, scaled by the other factors, so
     * that a reflected point which is itself a zero does not require dividing
     * by zero. The sine is handled separately because at the trivial zeros it
     * is meant to vanish, which makes its relative error meaningless; the
     * bound used there is the exact {@code |cos w| <= |sin w| + 1}, which
     * follows from {@code |cos w|^2 = |sin w|^2 + cos(2 Re w)}.</p>
     */
    private double errorBoundAt(
            Complex s,
            double valueMagnitude,
            double withoutReflectedZeta,
            double withoutSine,
            ZetaResult reflectedZeta) {

        double epsilon = Math.ulp(1.0);
        double height = Math.abs(s.imaginary());

        double gammaRelative = GAMMA_ROUNDOFF_TERMS * epsilon * (1.0 + height);

        double powerRelative = (s.magnitude() * LN_TWO
                + s.subtract(Complex.ONE).magnitude() * LN_PI
                + 2.0) * epsilon;

        double sineArgumentError = (Math.PI * s.magnitude() / 2.0) * epsilon;

        return valueMagnitude * (gammaRelative + powerRelative)
                + withoutReflectedZeta * reflectedZeta.estimatedErrorBound()
                + (valueMagnitude + withoutSine) * sineArgumentError;
    }

    private static boolean isFinite(Complex z) {
        return Double.isFinite(z.real()) && Double.isFinite(z.imaginary());
    }
}