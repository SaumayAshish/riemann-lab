package com.riemannlab.zeros;

import java.util.Objects;

/**
 * An immutable snapshot of the Micrometer metrics recorded for one scan mode
 * (sequential or parallel), read back from a
 * {@link io.micrometer.core.instrument.MeterRegistry}.
 *
 * <p>Every field defaults to zero when no matching meter exists yet - for
 * example, if only {@code scanParallel} has ever been called, the sequential
 * summary is all zeros rather than an error. A summary reports exactly what
 * {@link CriticalLineScanner} recorded; it never estimates or infers.
 *
 * @param mode the scan mode this summary describes ("sequential" or "parallel")
 * @param evaluationCount total zeta evaluations recorded across all scans of this mode
 * @param scanInvocationCount how many times a scan of this mode has been run
 * @param totalScanTimeMillis total wall-clock time spent scanning, summed across invocations
 * @param meanScanTimeMillis average wall-clock time per scan invocation
 * @param minimaFound total local minima detected across all scans of this mode
 * @param minimaDiscarded total local minima discarded as too shallow to be a plausible zero
 */
public record ScanMetricsSummary(
        String mode,
        long evaluationCount,
        long scanInvocationCount,
        double totalScanTimeMillis,
        double meanScanTimeMillis,
        long minimaFound,
        long minimaDiscarded) {

    /** Validates that mode is non-null. */
    public ScanMetricsSummary {
        Objects.requireNonNull(mode, "mode must not be null");
    }

    /**
     * The minima that survived plausibility filtering.
     *
     * @return minimaFound minus minimaDiscarded
     */
    public long minimaAccepted() {
        return minimaFound - minimaDiscarded;
    }
}