package com.riemannlab.app.cli;

import com.riemannlab.zeros.CriticalLineScanner;
import com.riemannlab.zeros.ZeroCandidate;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import java.util.List;

/**
 * Sweeps the critical line and reports every candidate zero it finds, using no
 * reference values at any point in the search.
 *
 * <p>Two passes are shown. The raw scan reports every strict local minimum of
 * {@code |zeta|}, which includes at least one genuine dip that is not a zero:
 * near t = 2.4 the magnitude falls to about 0.53 and then rises again, while
 * the first actual zero is at 14.13. The filtered scan keeps only the minima
 * whose drop from the larger neighbour is steep enough to place a zero inside
 * the bracket.</p>
 *
 * <p>The published heights printed in the comparison table are used only
 * afterwards, to check what the scan produced on its own. Recovering them is
 * numerical evidence that the evaluator and the detector work. It says nothing
 * about zeros that were not scanned, and it is not a proof of the Riemann
 * Hypothesis.</p>
 *
 * <p>The scan also cannot guarantee completeness: two zeros closer together
 * than the step size can cancel out of the local-minimum test entirely.
 * Guaranteeing that none were missed requires counting zeros independently,
 * which this project does not do.</p>
 */
public final class ZeroScanDemo {

    private static final double START_HEIGHT = 1.0;
    private static final double END_HEIGHT = 55.0;
    private static final double STEP = 0.1;

    private static final CriticalLineScanner SCANNER =
            new CriticalLineScanner(new AcceleratedEtaEvaluator());

    /**
     * Published reference values for the first eleven non-trivial zero heights,
     * used for validation only. These are established results, not discoveries.
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

    private ZeroScanDemo() {
        throw new AssertionError("ZeroScanDemo is an entry point and must not be instantiated");
    }
    /**
     * Demonstrates scanning the critical line for zero candidates.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        printHeader();

        long start = System.nanoTime();
        List<ZeroCandidate> allMinima = SCANNER.scan(START_HEIGHT, END_HEIGHT, STEP);
        List<ZeroCandidate> zeros = SCANNER.scanForZeros(START_HEIGHT, END_HEIGHT, STEP);
        long elapsedNanos = System.nanoTime() - start;

        printDiscrimination(allMinima, zeros);
        printCandidates(zeros);
        printComparison(zeros);
        printCost(zeros, elapsedNanos);
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("RiemannLab - candidate zeros on the critical line");
        System.out.println("=".repeat(92));
        System.out.printf("scanning s = 0.5 + it for t in [%.1f, %.1f], step %.2f%n",
                START_HEIGHT, END_HEIGHT, STEP);
        System.out.printf("average zero spacing near t = %.0f is about %.2f%n",
                END_HEIGHT, CriticalLineScanner.averageZeroSpacing(END_HEIGHT));
        System.out.println();
        System.out.println("No reference values are used during the search.");
        System.out.println("Numerical experimentation only. Nothing here is a proof.");
    }

    private static void printDiscrimination(
            List<ZeroCandidate> allMinima, List<ZeroCandidate> zeros) {

        section("Every local minimum, and which ones survive the plausibility test");
        System.out.printf("%4s  %14s  %12s  %14s  %10s%n",
                "#", "sampled t", "|zeta| at t", "relative drop", "verdict");
        System.out.println("-".repeat(92));

        int index = 1;
        for (ZeroCandidate candidate : allMinima) {
            System.out.printf("%4d  %14.4f  %12.3e  %14.4f  %10s%n",
                    index++,
                    candidate.height(),
                    candidate.magnitude(),
                    candidate.relativeDrop(),
                    candidate.looksLikeZero() ? "zero" : "mere dip");
        }

        System.out.println();
        System.out.printf("%d local minima, %d consistent with a zero, %d discarded.%n",
                allMinima.size(), zeros.size(), allMinima.size() - zeros.size());
        System.out.println("A discarded row is not an error. |zeta| genuinely dips there; it");
        System.out.println("simply does not come close enough to the axis for a zero to fit");
        System.out.println("inside the bracket. The linear model makes that judgement cheap.");
    }

    private static void printCandidates(List<ZeroCandidate> candidates) {
        section("Candidate zeros");
        System.out.printf("%4s  %14s  %16s  %12s  %12s  %10s%n",
                "#", "sampled t", "estimated t", "|zeta| at t", "slope |z'|", "bracket");
        System.out.println("-".repeat(92));

        int index = 1;
        for (ZeroCandidate candidate : candidates) {
            System.out.printf("%4d  %14.4f  %16.9f  %12.3e  %12.6f  %10.3f%n",
                    index++,
                    candidate.height(),
                    candidate.estimatedHeight(),
                    candidate.magnitude(),
                    candidate.estimatedSlope(),
                    candidate.bracketWidth());
        }
    }

    private static void printComparison(List<ZeroCandidate> candidates) {
        section("Comparison against published reference values");
        System.out.printf("%4s  %20s  %20s  %14s%n",
                "#", "estimated", "published", "difference");
        System.out.println("-".repeat(92));

        int comparable = Math.min(candidates.size(), REFERENCE_ZEROS.length);
        double worstDifference = 0.0;

        for (int i = 0; i < comparable; i++) {
            double estimated = candidates.get(i).estimatedHeight();
            double published = REFERENCE_ZEROS[i];
            double difference = Math.abs(estimated - published);
            worstDifference = Math.max(worstDifference, difference);

            System.out.printf("%4d  %20.9f  %20.9f  %14.2e%n",
                    i + 1, estimated, published, difference);
        }

        System.out.println();
        if (candidates.size() != REFERENCE_ZEROS.length) {
            System.out.printf("NOTE: found %d candidates but hold %d reference values.%n",
                    candidates.size(), REFERENCE_ZEROS.length);
        }
        System.out.printf("Largest difference: %.2e, from three grid samples per zero.%n",
                worstDifference);
        System.out.println("That is a starting point, not an answer. Driving these to machine");
        System.out.println("precision is the job of the root-finder in the next step.");
    }

    private static void printCost(List<ZeroCandidate> candidates, long elapsedNanos) {
        int samples = (int) Math.round((END_HEIGHT - START_HEIGHT) / STEP) + 1;

        section("Cost");
        System.out.printf("%-38s %12d%n", "zeta evaluations (two passes)", 2 * samples);
        System.out.printf("%-38s %12d%n", "candidate zeros found", candidates.size());
        System.out.printf("%-38s %12.1f%n", "milliseconds", elapsedNanos / 1e6);
        System.out.printf("%-38s %12.3f%n", "milliseconds per evaluation",
                elapsedNanos / 1e6 / (2.0 * samples));
        System.out.println();
        System.out.println("Scan cost is linear in the range divided by the step. The step in");
        System.out.println("turn must shrink like 1/ln(t) to keep resolving neighbouring zeros,");
        System.out.println("so the cost of scanning to height T grows a little faster than T.");
        System.out.println();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(92));
    }
}
