package com.riemannlab.zeros;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Reads the Micrometer metrics that {@link CriticalLineScanner} records and
 * turns them into a human-readable summary - the consumer side of Phase 7's
 * metrics work. Without this class, the counters and timer only prove
 * themselves inside unit tests; this is what lets a real run be inspected.
 *
 * <p>Lookups use {@link MeterRegistry#find(String)}, which returns {@code null}
 * on a miss, rather than {@link MeterRegistry#get(String)}, which throws.
 * Summarizing a mode that has never been scanned is a normal case here, not
 * an error.
 */
public final class ScanMetricsReporter {

    private ScanMetricsReporter() {
    }

    /**
     * Builds a {@link ScanMetricsSummary} for one scan mode by reading back the
     * counters and timer recorded under that mode's tag.
     *
     * @param registry the registry the scanner was constructed with
     * @param mode {@link CriticalLineScanner#MODE_SEQUENTIAL} or {@link CriticalLineScanner#MODE_PARALLEL}
     * @return the summary for that mode; zero-valued fields if nothing was recorded
     */
    public static ScanMetricsSummary summarize(MeterRegistry registry, String mode) {
        Objects.requireNonNull(registry, "registry must not be null");
        Objects.requireNonNull(mode, "mode must not be null");

        Counter evaluations = findCounter(registry, CriticalLineScanner.METRIC_EVALUATIONS, mode);
        Timer duration = findTimer(registry, CriticalLineScanner.METRIC_DURATION, mode);
        Counter found = findCounter(registry, CriticalLineScanner.METRIC_MINIMA_FOUND, mode);
        Counter discarded = findCounter(registry, CriticalLineScanner.METRIC_MINIMA_DISCARDED, mode);

        return new ScanMetricsSummary(
                mode,
                evaluations == null ? 0L : (long) evaluations.count(),
                duration == null ? 0L : duration.count(),
                duration == null ? 0.0 : duration.totalTime(TimeUnit.MILLISECONDS),
                duration == null ? 0.0 : duration.mean(TimeUnit.MILLISECONDS),
                found == null ? 0L : (long) found.count(),
                discarded == null ? 0L : (long) discarded.count());
    }

    /**
     * Formats the sequential and parallel summaries as a two-line, human-readable report.
     *
     * @param registry the registry the scanner was constructed with
     * @return the formatted report
     */
    public static String report(MeterRegistry registry) {
        ScanMetricsSummary sequential = summarize(registry, CriticalLineScanner.MODE_SEQUENTIAL);
        ScanMetricsSummary parallel = summarize(registry, CriticalLineScanner.MODE_PARALLEL);
        return "=== Scan Metrics Summary ===\n"
                + formatLine("Sequential", sequential) + "\n"
                + formatLine("Parallel", parallel);
    }

    private static String formatLine(String label, ScanMetricsSummary summary) {
        return String.format(
                "%-11s %d evaluations across %d scan(s), avg %.2f ms/scan (total %.2f ms) "
                        + "| %d minima found, %d discarded",
                label + ":",
                summary.evaluationCount(),
                summary.scanInvocationCount(),
                summary.meanScanTimeMillis(),
                summary.totalScanTimeMillis(),
                summary.minimaFound(),
                summary.minimaDiscarded());
    }

    private static Counter findCounter(MeterRegistry registry, String name, String mode) {
        return registry.find(name).tag(CriticalLineScanner.TAG_MODE, mode).counter();
    }

    private static Timer findTimer(MeterRegistry registry, String name, String mode) {
        return registry.find(name).tag(CriticalLineScanner.TAG_MODE, mode).timer();
    }
}