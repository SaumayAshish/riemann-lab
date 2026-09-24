package com.riemannlab.zeros;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Proves the parallel scan finds exactly the same candidates as the
 * sequential scan it is meant to replace - not "close enough," the identical
 * list, in the identical order. This is the correctness gate a parallel
 * speedup claim has to pass before it can be trusted.
 */
class CriticalLineScannerParallelCorrectnessTest {

    private static final double SCAN_START = 1.0;
    private static final double SCAN_END = 55.0;
    private static final double SCAN_STEP = 0.1;

    @Test
    void parallelScanMatchesSequentialScanExactly() {
        CriticalLineScanner scanner =
                new CriticalLineScanner(new ContinuedZetaEvaluator(new AcceleratedEtaEvaluator()));

        List<ZeroCandidate> sequential = scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);
        List<ZeroCandidate> parallel = scanner.scanParallel(SCAN_START, SCAN_END, SCAN_STEP);

        assertEquals(sequential, parallel);
    }

    @Test
    void parallelScanForZerosMatchesSequentialScanForZerosExactly() {
        CriticalLineScanner scanner =
                new CriticalLineScanner(new ContinuedZetaEvaluator(new AcceleratedEtaEvaluator()));

        List<ZeroCandidate> sequential = scanner.scanForZeros(SCAN_START, SCAN_END, SCAN_STEP);
        List<ZeroCandidate> parallel = scanner.scanForZerosParallel(SCAN_START, SCAN_END, SCAN_STEP);

        assertEquals(sequential, parallel);
    }
}