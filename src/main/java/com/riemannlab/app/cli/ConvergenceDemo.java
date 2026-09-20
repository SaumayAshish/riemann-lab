package com.riemannlab.app.cli;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.DirichletSeries;

/**
 * A runnable experiment demonstrating why the defining Dirichlet series cannot
 * be used to investigate the Riemann zeta function near the critical line.
 *
 * <p>Three cases are shown:</p>
 * <ol>
 *   <li>{@code s = 2} - the series converges, but the error only shrinks like
 *       {@code 1/N}, which is far too slow to reach useful precision.</li>
 *   <li>{@code s = 1} - the harmonic series, which diverges like
 *       {@code ln(N)}.</li>
 *   <li>{@code s = 0.5 + 14.134725i} - a known non-trivial zero, where the
 *       true value of zeta is zero but the partial sums grow like
 *       {@code sqrt(N)} and never settle.</li>
 * </ol>
 *
 * <p>This class writes to {@code System.out} deliberately: its entire purpose
 * is console output for a human reader. Production logic elsewhere in
 * RiemannLab logs through SLF4J instead.</p>
 */
public final class ConvergenceDemo {

    private static final int[] TERM_COUNTS = {10, 100, 1_000, 10_000, 100_000, 1_000_000};

    /** zeta(2) = pi^2 / 6, Euler 1735. A published reference value. */
    private static final double BASEL_CONSTANT = Math.PI * Math.PI / 6.0;

    /** The Euler-Mascheroni constant, the limit of H_N - ln(N). */
    private static final double EULER_MASCHERONI = 0.5772156649015329;

    /**
     * Imaginary part of the first non-trivial zero of zeta.
     * A published reference value used for validation, not a discovery.
     */
    private static final double FIRST_ZERO_HEIGHT = 14.134725141734693;

    private ConvergenceDemo() {
        throw new AssertionError("ConvergenceDemo is an entry point and must not be instantiated");
    }

    public static void main(String[] args) {
        System.out.println();
        System.out.println("RiemannLab - Dirichlet series convergence experiment");
        System.out.println("=".repeat(72));
        System.out.println("Numerical experimentation only. This proves nothing about the");
        System.out.println("Riemann Hypothesis; it measures how a known series behaves.");

        demonstrateSlowConvergence();
        demonstrateHarmonicDivergence();
        demonstrateCriticalLineDivergence();

        System.out.println();
        System.out.println("Conclusion");
        System.out.println("-".repeat(72));
        System.out.println("Re(s) > 1 : converges, but error ~ 1/N. Fifteen digits would need");
        System.out.println("            about 1e15 terms - and rounding error would win first.");
        System.out.println("Re(s) <= 1: does not converge at all. On the critical line, where");
        System.out.println("            every non-trivial zero lives, the defining series is");
        System.out.println("            not merely slow - it has no value to be slow about.");
        System.out.println();
        System.out.println("A different formula is required. That is the Dirichlet eta function.");
        System.out.println();
    }

    private static void demonstrateSlowConvergence() {
        section("CASE 1  s = 2 + 0i        Re(s) = 2 > 1, so the series converges");
        System.out.printf("target: pi^2/6 = %.16f%n%n", BASEL_CONSTANT);
        System.out.printf("%12s  %20s  %14s  %14s%n", "N", "partial sum", "error", "1/N");
        System.out.println("-".repeat(72));

        for (int termCount : TERM_COUNTS) {
            double sum = DirichletSeries.partialSum(Complex.ofReal(2), termCount).real();
            double error = Math.abs(BASEL_CONSTANT - sum);
            System.out.printf("%12d  %20.16f  %14.6e  %14.6e%n",
                    termCount, sum, error, 1.0 / termCount);
        }

        System.out.println();
        System.out.println("Read the last two columns: the error is tracking 1/N almost exactly.");
        System.out.println("Each extra correct digit costs ten times more work than the last.");
    }

    private static void demonstrateHarmonicDivergence() {
        section("CASE 2  s = 1 + 0i        the harmonic series, which diverges");
        System.out.printf("%12s  %20s  %20s%n", "N", "partial sum", "ln(N) + gamma");
        System.out.println("-".repeat(72));

        for (int termCount : TERM_COUNTS) {
            double sum = DirichletSeries.partialSum(Complex.ONE, termCount).real();
            System.out.printf("%12d  %20.12f  %20.12f%n",
                    termCount, sum, Math.log(termCount) + EULER_MASCHERONI);
        }

        System.out.println();
        System.out.println("The terms shrink to zero and the sum still grows without bound.");
        System.out.println("It tracks ln(N), so reaching 100 would take about 1.5e43 terms.");
    }

    private static void demonstrateCriticalLineDivergence() {
        section("CASE 3  s = 0.5 + 14.134725141734693i    a known non-trivial zero");
        System.out.println("The true value of zeta here is 0, so the true magnitude is 0.");
        System.out.println();
        System.out.printf("%12s  %32s  %12s  %12s%n", "N", "partial sum", "|sum|", "sqrt(N)");
        System.out.println("-".repeat(78));

        Complex s = Complex.of(0.5, FIRST_ZERO_HEIGHT);
        for (int termCount : TERM_COUNTS) {
            Complex sum = DirichletSeries.partialSum(s, termCount);
            System.out.printf("%12d  %32s  %12.6f  %12.2f%n",
                    termCount, format(sum), sum.magnitude(), Math.sqrt(termCount));
        }

        System.out.println();
        System.out.println("The magnitude should be heading to 0. Instead it grows with sqrt(N).");
        System.out.println("Adding more terms makes the answer worse, without limit.");
    }

    private static String format(Complex z) {
        return String.format("%.6f %s %.6fi",
                z.real(),
                z.imaginary() < 0 ? "-" : "+",
                Math.abs(z.imaginary()));
    }

    private static void section(String title) {
        System.out.println();
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(72));
    }
}
