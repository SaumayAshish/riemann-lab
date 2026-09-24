package com.riemannlab.validation;

import java.util.List;
import java.util.Objects;

/**
 * Aggregates a batch of {@link ZetaAgreementSample} measurements into the
 * handful of numbers that matter: how far apart the two independent
 * computations ever got, in absolute and relative terms.
 *
 * <p>Same role as {@link ResidualReport} one layer over: that class
 * summarizes whether a claimed zero survived refinement, this one
 * summarizes whether two unrelated ways of computing zeta agree with each
 * other at all.</p>
 */
public record ZetaAccuracyReport(List<ZetaAgreementSample> samples) {

    public ZetaAccuracyReport {
        Objects.requireNonNull(samples, "samples must not be null");
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("samples must not be empty");
        }
        samples = List.copyOf(samples);
    }

    public int count() {
        return samples.size();
    }

    /** The largest absolute difference seen across every sample. */
    public double worstAbsoluteDifference() {
        return samples.stream()
                .mapToDouble(ZetaAgreementSample::absoluteDifference)
                .max()
                .orElseThrow();
    }

    /** The mean absolute difference across every sample. */
    public double meanAbsoluteDifference() {
        return samples.stream()
                .mapToDouble(ZetaAgreementSample::absoluteDifference)
                .average()
                .orElseThrow();
    }

    /**
     * The largest relative difference among samples where a relative
     * difference is defined (the direct value was non-zero).
     *
     * <p>{@code NaN} if every sample landed exactly on a zero - which would
     * mean no sample in the batch could answer the question this method
     * asks.</p>
     */
    public double worstRelativeDifference() {
        return samples.stream()
                .mapToDouble(ZetaAgreementSample::relativeDifference)
                .filter(Double::isFinite)
                .max()
                .orElse(Double.NaN);
    }
}