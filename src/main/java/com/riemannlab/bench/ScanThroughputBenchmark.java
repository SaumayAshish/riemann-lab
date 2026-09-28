package com.riemannlab.bench;

import com.riemannlab.zeros.CriticalLineScanner;
import com.riemannlab.zeros.ZeroCandidate;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Throughput of {@link CriticalLineScanner}'s sequential and parallel scans
 * over the same fixed height range, so the two can be compared directly.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ScanThroughputBenchmark {

    /** Creates a benchmark instance. JMH constructs one per fork. */
    public ScanThroughputBenchmark() {
    }

    private static final double SCAN_START = 1.0;
    private static final double SCAN_END = 55.0;
    private static final double SCAN_STEP = 0.1;

    /** Per-iteration JMH state: a scanner rebuilt once per trial. */
    @State(Scope.Benchmark)
    public static class ScannerState {

        /** Creates an uninitialized state; JMH populates it via {@link #setUp()}. */
        public ScannerState() {
        }

        CriticalLineScanner scanner;

        /** Builds the scanner before each trial. */
        @Setup(Level.Trial)
        public void setUp() {
            scanner = new CriticalLineScanner(new ContinuedZetaEvaluator(new AcceleratedEtaEvaluator()));
        }
    }

    /**
     * Measures the sequential scan's throughput.
     *
     * @param state the shared benchmark state
     * @param blackhole consumes the result so the JIT cannot eliminate the call
     */
    @Benchmark
    public void scanForZeros(ScannerState state, Blackhole blackhole) {
        List<ZeroCandidate> candidates = state.scanner.scanForZeros(SCAN_START, SCAN_END, SCAN_STEP);
        blackhole.consume(candidates);
    }

    /**
     * Measures the parallel scan's throughput.
     *
     * @param state the shared benchmark state
     * @param blackhole consumes the result so the JIT cannot eliminate the call
     */
    @Benchmark
    public void scanForZerosParallel(ScannerState state, Blackhole blackhole) {
        List<ZeroCandidate> candidates = state.scanner.scanForZerosParallel(SCAN_START, SCAN_END, SCAN_STEP);
        blackhole.consume(candidates);
    }
}