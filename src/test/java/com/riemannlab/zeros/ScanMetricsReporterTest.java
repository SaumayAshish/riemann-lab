package com.riemannlab.zeros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScanMetricsReporterTest {

    private static final double SCAN_START = 1.0;
    private static final double SCAN_END = 55.0;
    private static final double SCAN_STEP = 0.1;
    private static final long EXPECTED_EVALUATIONS = 541L;
    private static final long EXPECTED_MINIMA_FOUND = 12L;
    private static final long EXPECTED_MINIMA_DISCARDED = 1L;

    private MeterRegistry registry;
    private CriticalLineScanner scanner;

    @BeforeEach
    void createScannerWithOwnRegistry() {
        registry = new SimpleMeterRegistry();
        scanner = new CriticalLineScanner(
                new ContinuedZetaEvaluator(new AcceleratedEtaEvaluator()), registry);
    }

    @Nested
    class SummarizeMethod {

        @Test
        void returnsAllZerosWhenModeHasNeverBeenScanned() {
            ScanMetricsSummary summary =
                    ScanMetricsReporter.summarize(registry, CriticalLineScanner.MODE_SEQUENTIAL);

            assertEquals(0L, summary.evaluationCount());
            assertEquals(0L, summary.scanInvocationCount());
            assertEquals(0.0, summary.totalScanTimeMillis());
            assertEquals(0.0, summary.meanScanTimeMillis());
            assertEquals(0L, summary.minimaFound());
            assertEquals(0L, summary.minimaDiscarded());
        }

        @Test
        void reflectsARecordedSequentialScan() {
            scanner.scanForZeros(SCAN_START, SCAN_END, SCAN_STEP);

            ScanMetricsSummary summary =
                    ScanMetricsReporter.summarize(registry, CriticalLineScanner.MODE_SEQUENTIAL);

            assertEquals(EXPECTED_EVALUATIONS, summary.evaluationCount());
            assertEquals(1L, summary.scanInvocationCount());
            assertEquals(EXPECTED_MINIMA_FOUND, summary.minimaFound());
            assertEquals(EXPECTED_MINIMA_DISCARDED, summary.minimaDiscarded());
            assertEquals(EXPECTED_MINIMA_FOUND - EXPECTED_MINIMA_DISCARDED, summary.minimaAccepted());
            assertTrue(summary.totalScanTimeMillis() > 0.0, "a real scan takes non-zero time");
            assertEquals(summary.totalScanTimeMillis(), summary.meanScanTimeMillis(),
                    "mean equals total when there is exactly one invocation");
        }

        @Test
        void reflectsARecordedParallelScan() {
            scanner.scanForZerosParallel(SCAN_START, SCAN_END, SCAN_STEP);

            ScanMetricsSummary summary =
                    ScanMetricsReporter.summarize(registry, CriticalLineScanner.MODE_PARALLEL);

            assertEquals(EXPECTED_EVALUATIONS, summary.evaluationCount());
            assertEquals(1L, summary.scanInvocationCount());
            assertEquals(EXPECTED_MINIMA_FOUND, summary.minimaFound());
            assertEquals(EXPECTED_MINIMA_DISCARDED, summary.minimaDiscarded());
        }
    }

    @Nested
    class ReportMethod {

        @Test
        void includesBothModesEvenWhenOnlyOneWasScanned() {
            scanner.scanForZeros(SCAN_START, SCAN_END, SCAN_STEP);

            String report = ScanMetricsReporter.report(registry);

            assertTrue(report.contains("Sequential:"));
            assertTrue(report.contains("Parallel:"));
            assertTrue(report.contains(EXPECTED_EVALUATIONS + " evaluations"),
                    "sequential's real evaluation count should appear");
            assertTrue(report.contains("0 evaluations"),
                    "parallel was never scanned, so it should report zero, not throw");
        }
    }
}