package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The eta identity driven by direct summation, with no acceleration.
 *
 * <p>Far slower and far less accurate than {@link AcceleratedEtaEvaluator} -
 * its error falls off only like {@code N^-Re(s)}, so on the critical line
 * twenty thousand terms buy about three decimal places.</p>
 *
 * <p>It is kept for two reasons. It shares no machinery with the acceleration
 * algorithm, so agreement between the two is genuinely independent evidence
 * rather than two views of the same code. And it makes the cost of
 * acceleration measurable instead of asserted.</p>
 *
 * <p>Immutable and therefore thread-safe.</p>
 */
public final class NaiveEtaEvaluator implements ZetaEvaluator {

    private static final Logger log = LoggerFactory.getLogger(NaiveEtaEvaluator.class);

    private final int termCount;

    /**
     * Creates an evaluator summing a fixed number of eta terms.
     *
     * @param termCount how many terms to sum; must be at least 1
     * @throws IllegalArgumentException if {@code termCount} is below 1
     */
    public NaiveEtaEvaluator(int termCount) {
        if (termCount < 1) {
            throw new IllegalArgumentException("termCount must be at least 1, was " + termCount);
        }
        this.termCount = termCount;
        log.info("Created {}", name());
    }

    @Override
    public ZetaResult evaluate(Complex s) {
        Complex denominator = EtaIdentity.denominatorAt(s);

        Complex value = EtaFunction.partialSum(s, termCount).divide(denominator);

        return new ZetaResult(value, termCount, errorBoundAt(s, denominator.magnitude()));
    }

    /**
     * The number of terms this evaluator sums.
     *
     * @return the configured term count
     */
    public int termCount() {
        return termCount;
    }

    @Override
    public String name() {
        return String.format("naive-eta(%,d terms)", termCount);
    }

    /**
     * By the alternating series test the truncation error is bounded by the
     * size of the next term, {@code N^-Re(s)}. Accumulated rounding across
     * {@code N} additions is bounded by {@code N} machine epsilons, which
     * dominates once the series itself converges quickly.
     */
    private double errorBoundAt(Complex s, double denominatorMagnitude) {
        double truncation = Math.pow(termCount, -s.real());
        double roundoff = termCount * Math.ulp(1.0);

        return Math.max(truncation, roundoff) / denominatorMagnitude;
    }
}