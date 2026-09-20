package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;

/**
 * A strategy for evaluating the Riemann zeta function.
 *
 * <p><strong>Contract.</strong> Every implementation must:</p>
 * <ul>
 *   <li>accept any {@code s} with {@code Re(s) > 0} that is not a singularity
 *       of the eta identity, and reject anything else;</li>
 *   <li>throw {@link IllegalArgumentException} for {@code Re(s) <= 0}, or for
 *       arguments beyond its own supported range;</li>
 *   <li>throw {@link ArithmeticException} at {@code s = 1} and at the
 *       removable singularities {@code s = 1 + 2*pi*i*k/ln(2)};</li>
 *   <li>report an {@code estimatedErrorBound} that its own results actually
 *       respect;</li>
 *   <li>be deterministic and safe to call from multiple threads.</li>
 * </ul>
 *
 * <p>{@code DirichletSeries} deliberately does not implement this interface.
 * It converges only for {@code Re(s) > 1}, so substituting it would silently
 * return divergent garbage on the critical line - a Liskov violation that the
 * shared contract test would catch.</p>
 */
public interface ZetaEvaluator {

    /**
     * Evaluates zeta at {@code s}, reporting the value alongside the cost and
     * the accuracy of the computation.
     *
     * @param s the point at which to evaluate zeta
     * @return the value and its diagnostics
     * @throws IllegalArgumentException if {@code s} is outside this
     *                                  evaluator's supported domain
     * @throws ArithmeticException      if {@code s} is a singularity
     */
    ZetaResult evaluate(Complex s);

    /**
     * A short human-readable identification of this evaluator and its
     * configuration, for logs, reports and test failure messages.
     *
     * @return the evaluator's name
     */
    String name();

    /**
     * Convenience shorthand for callers that need only the value.
     *
     * @param s the point at which to evaluate zeta
     * @return the value of zeta at {@code s}
     */
    default Complex valueAt(Complex s) {
        return evaluate(s).value();
    }
}