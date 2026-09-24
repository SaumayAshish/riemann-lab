package com.riemannlab.validation;

import java.util.List;
import java.util.Objects;

/**
 * Aggregates a batch of {@link PrecisionSample} measurements into whether
 * an evaluator's error bound can be trusted: the worst ratio of actual
 * error to claimed bound, and how many samples broke the bound outright.
 */
public record PrecisionReport(List<PrecisionSample> samples) {

    public PrecisionReport {
        Objects.requireNonNull(samples, "samples must not be null");
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("samples must not be empty");
        }
        samples = List.copyOf(samples);
    }

    public int count() {
        return samples.size();
    }

    /** The largest ratio of actual error to claimed bound across every sample. */
    public double worstErrorRatio() {
        return samples.stream()
                .mapToDouble(PrecisionSample::errorRatio)
                .max()
                .orElseThrow();
    }

    /** How many samples exceeded the error bound the evaluator claimed for itself. */
    public long countExceedingClaimedBound() {
        return samples.stream().filter(sample -> !sample.isWithinClaimedBound()).count();
    }

    /** Whether every sample's actual error stayed within its claimed bound. */
    public boolean allWithinClaimedBound() {
        return countExceedingClaimedBound() == 0;
    }
}
