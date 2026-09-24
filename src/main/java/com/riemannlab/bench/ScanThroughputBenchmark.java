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

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ScanThroughputBenchmark {

    private static final double SCAN_START = 1.0;
    private static final double SCAN_END = 55.0;
    private static final double SCAN_STEP = 0.1;

    @State(Scope.Benchmark)
    public static class ScannerState {

        CriticalLineScanner scanner;

        @Setup(Level.Trial)
        public void setUp() {
            scanner = new CriticalLineScanner(new ContinuedZetaEvaluator(new AcceleratedEtaEvaluator()));
        }
    }

    @Benchmark
    public void scanForZeros(ScannerState state, Blackhole blackhole) {
        List<ZeroCandidate> candidates = state.scanner.scanForZeros(SCAN_START, SCAN_END, SCAN_STEP);
        blackhole.consume(candidates);
    }

    @Benchmark
    public void scanForZerosParallel(ScannerState state, Blackhole blackhole) {
        List<ZeroCandidate> candidates = state.scanner.scanForZerosParallel(SCAN_START, SCAN_END, SCAN_STEP);
        blackhole.consume(candidates);
    }
}