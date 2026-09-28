package com.riemannlab.bench;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import com.riemannlab.zeta.NaiveEtaEvaluator;
import com.riemannlab.zeta.ZetaEvaluator;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Baseline throughput of the three zeta evaluators, on the critical line
 * at a few heights where their behaviour differs most: the naive
 * evaluator's cost is fixed by its configured term count regardless of
 * height, while the accelerated evaluator's term count - and therefore
 * its cost - grows with height.
 *
 * <p>This benchmark changes nothing about the evaluators; it only measures
 * what already exists, honestly. It is characterization, not optimization
 * - nothing in this phase's optimization step touches these classes.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class EvaluatorThroughputBenchmark {

    /** Creates a benchmark instance. JMH constructs one per fork. */
    public EvaluatorThroughputBenchmark() {
    }

    /**
     * Term count for the naive evaluator. Its own JavaDoc notes that on
     * the critical line, twenty thousand terms buy about three decimal
     * places - a realistic configuration rather than a toy one, without
     * making this benchmark itself slow to run.
     */
    private static final int NAIVE_TERM_COUNT = 20_000;

    /**
     * Per-iteration JMH state: the point to evaluate at, and one
     * evaluator of each kind, rebuilt once per trial rather than once per
     * invocation so the benchmark measures evaluation, not construction.
     */
    @State(Scope.Benchmark)
    public static class Evaluators {

        /** Creates an uninitialized state; JMH populates it via {@link #setUp()}. */
        public Evaluators() {
        }

        /** The imaginary part of the point on the critical line to evaluate at. */
        @Param({"10.0", "50.0", "100.0"})
        public double height;

        /** The point {@code 1/2 + i * height} shared by all three evaluators. */
        public Complex point;

        /** Evaluates eta by direct summation of {@link #NAIVE_TERM_COUNT} terms. */
        public ZetaEvaluator naive;

        /** Evaluates eta using convergence acceleration. */
        public ZetaEvaluator accelerated;

        /** Evaluates zeta via analytic continuation, backed by {@link #accelerated}. */
        public ZetaEvaluator continued;

        /** Builds {@link #point} and the three evaluators before each trial. */
        @Setup(Level.Trial)
        public void setUp() {
            point = Complex.of(0.5, height);
            naive = new NaiveEtaEvaluator(NAIVE_TERM_COUNT);
            accelerated = new AcceleratedEtaEvaluator();
            continued = new ContinuedZetaEvaluator(accelerated);
        }
    }

    /**
     * Measures the naive evaluator's throughput.
     *
     * @param state the shared benchmark state
     * @param blackhole consumes the result so the JIT cannot eliminate the call
     */
    @Benchmark
    public void naiveEta(Evaluators state, Blackhole blackhole) {
        blackhole.consume(state.naive.evaluate(state.point));
    }

    /**
     * Measures the accelerated evaluator's throughput.
     *
     * @param state the shared benchmark state
     * @param blackhole consumes the result so the JIT cannot eliminate the call
     */
    @Benchmark
    public void acceleratedEta(Evaluators state, Blackhole blackhole) {
        blackhole.consume(state.accelerated.evaluate(state.point));
    }

    /**
     * Measures the continued evaluator's throughput.
     *
     * @param state the shared benchmark state
     * @param blackhole consumes the result so the JIT cannot eliminate the call
     */
    @Benchmark
    public void continuedZeta(Evaluators state, Blackhole blackhole) {
        blackhole.consume(state.continued.evaluate(state.point));
    }
}