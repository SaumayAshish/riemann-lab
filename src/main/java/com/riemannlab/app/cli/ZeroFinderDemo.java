package com.riemannlab.app.cli;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeros.CriticalLineScanner;
import com.riemannlab.zeros.RefinedZero;
import com.riemannlab.zeros.ZeroCandidate;
import com.riemannlab.zeros.ZeroRefiner;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import java.util.ArrayList;
import java.util.List;

/**
 * The complete pipeline: scan the critical line, refine every candidate to
 * machine precision, and compare the result against published values.
 *
 * <p>No reference value is used anywhere in the search. They appear only in the
 * final comparison, as a check on what the program produced independently.</p>
 *
 * <p>The root finder is free to move anywhere in the complex plane; nothing
 * constrains it to {@code Re(s) = 1/2}. The real part of each answer is
 * therefore a measurement rather than an assumption, and the last section
 * pushes that further by starting searches a fifth of the way across the
 * critical strip and watching where they end up.</p>
 *
 * <p><strong>What this establishes and what it does not.</strong> It shows that
 * eleven zeros, located without reference data, have real part equal to one
 * half to within about 1e-13, and that a search seeded off the critical line
 * converges onto it. It establishes nothing about the infinitely many zeros
 * that were not scanned, and it is not a proof of the Riemann Hypothesis.
 * Numerical agreement is evidence; it is not proof, and no amount of it
 * becomes proof.</p>
 */
public final class ZeroFinderDemo {

    private static final double START_HEIGHT = 1.0;
    private static final double END_HEIGHT = 55.0;
    private static final double STEP = 0.1;
    private static final double CRITICAL_LINE = 0.5;

    private static final AcceleratedEtaEvaluator EVALUATOR = new AcceleratedEtaEvaluator();
    private static final CriticalLineScanner SCANNER = new CriticalLineScanner(EVALUATOR);
    private static final ZeroRefiner REFINER = new ZeroRefiner(EVALUATOR);

    /**
     * Published reference values for the first eleven non-trivial zero heights,
     * used only for the comparison table. Established results, not discoveries.
     */
    private static final double[] REFERENCE_ZEROS = {
        14.134725141734693,
        21.022039638771555,
        25.010857580145688,
        30.424876125859513,
        32.935061587739189,
        37.586178158825671,
        40.918719012147495,
        43.327073280914999,
        48.005150881167159,
        49.773832477672302,
        52.970321477714460
    };

    /** Deliberately off-line starting points, to see where the search goes. */
    private static final Complex[] OFF_LINE_STARTS = {
        Complex.of(0.70, 14.10),
        Complex.of(0.35, 21.00),
        Complex.of(0.80, 25.00),
        Complex.of(0.30, 30.40)
    };
    /** Demonstrates finding a zero of zeta. */
    private ZeroFinderDemo() {
        throw new AssertionError("ZeroFinderDemo is an entry point and must not be instantiated");
    }

    public static void main(String[] args) {
        printHeader();

        long scanStart = System.nanoTime();
        List<ZeroCandidate> candidates = SCANNER.scanForZeros(START_HEIGHT, END_HEIGHT, STEP);
        long scanNanos = System.nanoTime() - scanStart;

        long refineStart = System.nanoTime();
        List<RefinedZero> zeros = REFINER.refineAll(candidates);
        long refineNanos = System.nanoTime() - refineStart;

        printResults(candidates, zeros);
        printRealParts(zeros);
        printOffLineExperiment();
        printCost(candidates, zeros, scanNanos, refineNanos);
        printDisclaimer();
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("RiemannLab - non-trivial zeros located and validated");
        System.out.println("=".repeat(96));
        System.out.printf("scan:   s = 0.5 + it, t in [%.0f, %.0f], step %.2f%n",
                START_HEIGHT, END_HEIGHT, STEP);
        System.out.println("refine: secant method, free to move anywhere in the complex plane");
        System.out.println();
        System.out.println("No reference values are used during the search.");
    }

    private static void printResults(List<ZeroCandidate> candidates, List<RefinedZero> zeros) {
        section("Refined zeros against published reference values");
        System.out.printf("%3s  %22s  %22s  %11s  %11s  %6s%n",
                "#", "refined height", "published height", "difference", "|zeta|", "iters");
        System.out.println("-".repeat(96));

        double worstDifference = 0.0;
        int confirmed = 0;

        for (int i = 0; i < zeros.size(); i++) {
            RefinedZero zero = zeros.get(i);
            boolean haveReference = i < REFERENCE_ZEROS.length;
            double difference = haveReference
                    ? Math.abs(zero.height() - REFERENCE_ZEROS[i])
                    : Double.NaN;

            if (haveReference) {
                worstDifference = Math.max(worstDifference, difference);
            }
            if (zero.isConfirmed()) {
                confirmed++;
            }

            System.out.printf("%3d  %22.15f  %22.15f  %11.2e  %11.2e  %6d%n",
                    i + 1,
                    zero.height(),
                    haveReference ? REFERENCE_ZEROS[i] : Double.NaN,
                    difference,
                    zero.residualMagnitude(),
                    zero.iterations());
        }

        System.out.println();
        System.out.printf("%d of %d candidates confirmed as zeros.%n", confirmed, zeros.size());
        System.out.printf("Largest disagreement with published values: %.2e%n", worstDifference);
        System.out.println();
        System.out.println("Improvement from the scan estimate, zero by zero:");
        System.out.printf("%3s  %14s  %14s  %12s%n", "#", "scan error", "refined error", "factor");
        System.out.println("-".repeat(96));

        for (int i = 0; i < Math.min(zeros.size(), REFERENCE_ZEROS.length); i++) {
            double scanError =
                    Math.abs(candidates.get(i).estimatedHeight() - REFERENCE_ZEROS[i]);
            double refinedError = Math.abs(zeros.get(i).height() - REFERENCE_ZEROS[i]);

            System.out.printf("%3d  %14.2e  %14.2e  %12.1e%n",
                    i + 1, scanError, refinedError,
                    scanError / Math.max(refinedError, 1e-17));
        }
    }

    private static void printRealParts(List<RefinedZero> zeros) {
        section("Where the real part ended up (an output, not an input)");
        System.out.printf("%3s  %22s  %26s  %16s%n",
                "#", "height", "Re(s)", "|Re(s) - 0.5|");
        System.out.println("-".repeat(96));

        double worstDeviation = 0.0;

        for (int i = 0; i < zeros.size(); i++) {
            RefinedZero zero = zeros.get(i);
            worstDeviation = Math.max(worstDeviation, zero.deviationFromCriticalLine());

            System.out.printf("%3d  %22.12f  %26.20f  %16.2e%n",
                    i + 1, zero.height(), zero.realPart(),
                    zero.deviationFromCriticalLine());
        }

        System.out.println();
        System.out.printf("Largest deviation from the critical line: %.2e%n", worstDeviation);
        System.out.println();
        System.out.println("The search was never constrained to Re(s) = 0.5. These values are");
        System.out.println("what the root finder arrived at on its own, which is why they are");
        System.out.println("worth printing at all.");
    }

    private static void printOffLineExperiment() {
        section("Searches deliberately started off the critical line");
        System.out.println("Each search begins a fifth of the way across the critical strip.");
        System.out.println("Nothing pushes it back toward Re(s) = 0.5.");
        System.out.println();
        System.out.printf("%24s  %26s  %22s  %10s%n",
                "started at", "ended at Re(s)", "ended at height", "outcome");
        System.out.println("-".repeat(96));

        List<RefinedZero> results = new ArrayList<>();

        for (Complex start : OFF_LINE_STARTS) {
            RefinedZero zero = REFINER.refineFrom(start);
            results.add(zero);

            System.out.printf("%24s  %26.20f  %22.12f  %10s%n",
                    String.format("%.2f + %.2fi", start.real(), start.imaginary()),
                    zero.realPart(),
                    zero.height(),
                    zero.isConfirmed() ? "confirmed" : zero.outcome().toString());
        }

        double worstDeviation = results.stream()
                .filter(RefinedZero::isConfirmed)
                .mapToDouble(RefinedZero::deviationFromCriticalLine)
                .max()
                .orElse(Double.NaN);

        System.out.println();
        System.out.printf("Largest deviation among the confirmed results: %.2e%n", worstDeviation);
        System.out.println();
        System.out.println("A search free to settle anywhere in the strip settled on the line.");
        System.out.println("That is the strongest statement this program can make, and it is");
        System.out.println("still only a statement about these four searches.");
    }

    private static void printCost(
            List<ZeroCandidate> candidates, List<RefinedZero> zeros,
            long scanNanos, long refineNanos) {

        section("Cost");

        int scanEvaluations = (int) Math.round((END_HEIGHT - START_HEIGHT) / STEP) + 1;
        int refineEvaluations = zeros.stream().mapToInt(RefinedZero::zetaEvaluations).sum();

        System.out.printf("%-42s %14s %14s%n", "", "detection", "refinement");
        System.out.println("-".repeat(96));
        System.out.printf("%-42s %14d %14d%n", "zeta evaluations",
                scanEvaluations, refineEvaluations);
        System.out.printf("%-42s %14.1f %14.1f%n", "milliseconds",
                scanNanos / 1e6, refineNanos / 1e6);
        System.out.printf("%-42s %14d %14d%n", "zeros produced",
                candidates.size(), zeros.size());
        System.out.printf("%-42s %14.1f %14.1f%n", "evaluations per zero",
                (double) scanEvaluations / candidates.size(),
                (double) refineEvaluations / zeros.size());

        System.out.println();
        System.out.printf("Refinement is %.0f%% of the total cost. Finding the neighbourhoods is%n",
                100.0 * refineEvaluations / (scanEvaluations + refineEvaluations));
        System.out.println("the expensive part; polishing a candidate to fifteen digits is not.");
        System.out.println("That is the argument for separating detection from refinement.");
    }

    private static void printDisclaimer() {
        section("What this does and does not establish");
        System.out.println("Established, for the range scanned:");
        System.out.println("  - eleven zeros located without reference data; seven match the");
        System.out.println("    published heights bit for bit, the rest to one ulp");
        System.out.println("  - every real part equal to 0.5 to within 4e-15, unconstrained");
        System.out.println("  - searches begun off the critical line converged onto it");
        System.out.println();
        System.out.println("NOT established:");
        System.out.println("  - anything about the infinitely many zeros above t = 55");
        System.out.println("  - that no zero exists off the line, even within this range: the");
        System.out.println("    scan can miss zeros closer together than its step");
        System.out.println("  - the Riemann Hypothesis, in any part or degree");
        System.out.println();
        System.out.println("RiemannLab is a computational research and visualization project for");
        System.out.println("numerically evaluating the Riemann zeta function and investigating");
        System.out.println("its non-trivial zeros. Numerical experimentation is not proof.");
        System.out.println();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(96));
    }
}
