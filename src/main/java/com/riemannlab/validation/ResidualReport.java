package com.riemannlab.validation;

import com.riemannlab.zeros.RefinedZero;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A summary of how a batch of {@link RefinedZero} results turned out: how
 * many converged, how many failed and why, and the residual statistics for
 * the ones that were confirmed.
 *
 * <p>This class does not judge anything. {@link RefinedZero.Outcome},
 * decided by {@code ZeroRefiner} at the moment each zero was refined, is
 * still the sole authority on whether a candidate is a zero. A
 * {@code ResidualReport} only counts and summarizes outcomes that already
 * happened, so a demo or a test can ask "how did this batch do" as one
 * structured object instead of re-deriving the same statistics from a
 * printed table.</p>
 *
 * @param entries the batch of refinement results this report summarizes
 */
public record ResidualReport(List<RefinedZero> entries) {

    /** Validates that {@code entries} is non-null and non-empty, and defensively copies it. */
    public ResidualReport {
        Objects.requireNonNull(entries, "entries must not be null");
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries must not be empty");
        }
    }

    /** How many entries this report covers, regardless of outcome. */
    public int total() {
        return entries.size();
    }

    /** How many entries had the given outcome. */
    public long countOf(RefinedZero.Outcome outcome) {
        return entries.stream().filter(entry -> entry.outcome() == outcome).count();
    }

    /** A count for every possible outcome, including ones that did not occur. */
    public Map<RefinedZero.Outcome, Long> outcomeCounts() {
        Map<RefinedZero.Outcome, Long> counts = new EnumMap<>(RefinedZero.Outcome.class);
        for (RefinedZero.Outcome outcome : RefinedZero.Outcome.values()) {
            counts.put(outcome, countOf(outcome));
        }
        return counts;
    }

    /** Only the entries whose outcome was {@link RefinedZero.Outcome#CONFIRMED}. */
    public List<RefinedZero> confirmed() {
        return entries.stream().filter(RefinedZero::isConfirmed).toList();
    }

    /**
     * The largest residual among confirmed zeros - the weakest case in this
     * batch for "this is really a zero." {@code NaN} if nothing here was
     * confirmed.
     */
    public double worstConfirmedResidual() {
        return confirmed().stream()
                .mapToDouble(RefinedZero::residualMagnitude)
                .max()
                .orElse(Double.NaN);
    }

    /**
     * The largest ratio of residual to the evaluator's own error bound,
     * among confirmed zeros - how close the weakest confirmation came to
     * the line between "confirmed" and "residual above bound." Assumes each
     * confirmed entry's error bound is positive, which is how every
     * evaluator in this project computes it. {@code NaN} if nothing here
     * was confirmed.
     */
    public double worstConfirmedResidualRatio() {
        return confirmed().stream()
                .mapToDouble(entry -> entry.residualMagnitude() / entry.evaluatorErrorBound())
                .max()
                .orElse(Double.NaN);
    }

    /**
     * The largest distance from the critical line among confirmed zeros.
     * {@code NaN} if nothing here was confirmed.
     */
    public double worstDeviationFromCriticalLine() {
        return confirmed().stream()
                .mapToDouble(RefinedZero::deviationFromCriticalLine)
                .max()
                .orElse(Double.NaN);
    }
}