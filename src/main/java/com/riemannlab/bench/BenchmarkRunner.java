package com.riemannlab.bench;

/**
 * Launches every JMH benchmark in this project. JMH generates its own
 * runner infrastructure at compile time from the {@code @Benchmark}
 * methods it finds on the classpath - this class only hands control to
 * that generated machinery.
 */
public final class BenchmarkRunner {

    private BenchmarkRunner() {
        throw new AssertionError("BenchmarkRunner is an entry point and must not be instantiated");
    }
    // bench/BenchmarkRunner.java
    /**
     * Entry point that runs the JMH benchmark suite.
     *
     * @param args unused
     * @throws Exception if the benchmark run fails
     */
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
