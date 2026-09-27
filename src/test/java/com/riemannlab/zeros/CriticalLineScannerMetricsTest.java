package com.riemannlab.zeros;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Proves that {@link CriticalLineScanner} records the metrics it claims to:
 * an evaluations counter, a scan-duration timer, and found/discarded minima
 * counters, each tagged by scan mode so the two paths can be queried
 * independently from one registry.
 *
 * <p>Each test builds the scanner with its own private
 * {@link SimpleMeterRegistry} - the same kind the scanner would create for
 * itself by default - but holds onto the reference so the recorded numbers
 * can be asserted afterward.</p>
 */
class CriticalLineScannerMetricsTest {

    private static final double SCAN_START = 1.0;
    private static final double SCAN_END = 55.0;
    private static final double SCAN_STEP = 0.1;
    private static final double EXPECTED_EVALUATIONS = 541.0;
    private static final double EXPECTED_MINIMA_FOUND = 12.0;
    private static final double EXPECTED_MINIMA_DISCARDED = 1.0;

    private MeterRegistry registry;
    private CriticalLineScanner scanner;

    @BeforeEach
    void createScannerWithOwnRegistry() {
        registry = new SimpleMeterRegistry();
        scanner = new CriticalLineScanner(
                new ContinuedZetaEvaluator(new AcceleratedEtaEvaluator()), registry);
    }

    @Nested
    class EvaluationsCounter {

        @Test
        void sequentialScanTagsEvaluationsAsSequential() {
            scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);

            double count = registry.get("riemannlab.scan.evaluations")
                    .tag("mode", "sequential")
                    .counter()
                    .count();

            assertEquals(EXPECTED_EVALUATIONS, count);
        }

        @Test
        void parallelScanTagsEvaluationsAsParallel() {
            scanner.scanParallel(SCAN_START, SCAN_END, SCAN_STEP);

            double count = registry.get("riemannlab.scan.evaluations")
                    .tag("mode", "parallel")
                    .counter()
                    .count();

            assertEquals(EXPECTED_EVALUATIONS, count);
        }

        @Test
        void sequentialAndParallelCountsAreIndependent() {
            scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);
            scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);
            scanner.scanParallel(SCAN_START, SCAN_END, SCAN_STEP);

            double sequentialCount = registry.get("riemannlab.scan.evaluations")
                    .tag("mode", "sequential")
                    .counter()
                    .count();
            double parallelCount = registry.get("riemannlab.scan.evaluations")
                    .tag("mode", "parallel")
                    .counter()
                    .count();

            assertEquals(EXPECTED_EVALUATIONS * 2, sequentialCount,
                    "two sequential scans should accumulate, not reset");
            assertEquals(EXPECTED_EVALUATIONS, parallelCount,
                    "the one parallel scan should be unaffected by the sequential calls");
        }
    }

    @Nested
    class DurationTimer {

        @Test
        void sequentialScanRecordsOneTimingTaggedSequential() {
            scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);

            long recordedCalls = registry.get("riemannlab.scan.duration")
                    .tag("mode", "sequential")
                    .timer()
                    .count();

            assertEquals(1, recordedCalls);
        }

        @Test
        void parallelScanRecordsOneTimingTaggedParallel() {
            scanner.scanParallel(SCAN_START, SCAN_END, SCAN_STEP);

            long recordedCalls = registry.get("riemannlab.scan.duration")
                    .tag("mode", "parallel")
                    .timer()
                    .count();

            assertEquals(1, recordedCalls);
        }

        @Test
        void repeatedScansAccumulateInTheSameTimer() {
            scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);
            scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);
            scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);

            long recordedCalls = registry.get("riemannlab.scan.duration")
                    .tag("mode", "sequential")
                    .timer()
                    .count();

            assertEquals(3, recordedCalls);
        }
    }

    @Nested
    class MinimaCounters {

        @Test
        void scanForZerosRecordsFoundAndDiscardedTaggedSequential() {
            List<ZeroCandidate> result = scanner.scanForZeros(SCAN_START, SCAN_END, SCAN_STEP);

            double found = registry.get("riemannlab.scan.minima.found")
                    .tag("mode", "sequential")
                    .counter()
                    .count();
            double discarded = registry.get("riemannlab.scan.minima.discarded")
                    .tag("mode", "sequential")
                    .counter()
                    .count();

            assertEquals(EXPECTED_MINIMA_FOUND, found);
            assertEquals(EXPECTED_MINIMA_DISCARDED, discarded);
            assertEquals(EXPECTED_MINIMA_FOUND - EXPECTED_MINIMA_DISCARDED, result.size(),
                    "the plausible candidates returned should be found minus discarded");
        }

        @Test
        void scanForZerosParallelRecordsFoundAndDiscardedTaggedParallel() {
            List<ZeroCandidate> result = scanner.scanForZerosParallel(SCAN_START, SCAN_END, SCAN_STEP);

            double found = registry.get("riemannlab.scan.minima.found")
                    .tag("mode", "parallel")
                    .counter()
                    .count();
            double discarded = registry.get("riemannlab.scan.minima.discarded")
                    .tag("mode", "parallel")
                    .counter()
                    .count();

            assertEquals(EXPECTED_MINIMA_FOUND, found);
            assertEquals(EXPECTED_MINIMA_DISCARDED, discarded);
            assertEquals(EXPECTED_MINIMA_FOUND - EXPECTED_MINIMA_DISCARDED, result.size());
        }
    }
}