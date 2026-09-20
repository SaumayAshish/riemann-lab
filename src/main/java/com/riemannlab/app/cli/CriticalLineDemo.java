package com.riemannlab.app.cli;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.ZetaFunction;

/**
 * Scans the critical line {@code Re(s) = 1/2} and prints the magnitude of
 * zeta at each height, so the non-trivial zeros become visible as dips.
 *
 * <p>This is the first point in RiemannLab where the zeta function is
 * evaluated somewhere the defining series cannot reach. The evaluator is the
 * eta identity, whose accuracy here is only about three decimal places - good
 * enough to see a zero, nowhere near good enough to locate one. Locating is
 * the job of later phases.</p>
 *
 * <p>The reference heights printed at the end are published values used for
 * validation. Agreement with them is numerical evidence and nothing more.
 * This program does not prove anything about the Riemann Hypothesis.</p>
 */
public final class CriticalLineDemo {

    private static final double START_HEIGHT = 10.0;
    private static final double END_HEIGHT = 30.0;
    private static final double STEP = 0.25;
    private static final int TERM_COUNT = 20_000;
    private static final double CRITICAL_LINE = 0.5;

    private static final int BAR_WIDTH = 46;
    private static final double BAR_SCALE = 14.0;

    /** Published reference values for the first three non-trivial zero heights. */
    private static final double[] REFERENCE_ZEROS = {
        14.134725141734693,
        21.022039638771555,
        25.010857580145688
    };

    private CriticalLineDemo() {
        throw new AssertionError("CriticalLineDemo is an entry point and must not be instantiated");
    }

    public static void main(String[] args) {
        printHeader();
        scanTheCriticalLine();
        compareAgainstReferenceValues();
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("RiemannLab - magnitude of zeta along the critical line");
        System.out.println("=".repeat(78));
        System.out.printf("s = 0.5 + it,  t from %.1f to %.1f in steps of %.2f%n",
                START_HEIGHT, END_HEIGHT, STEP);
        System.out.printf("evaluator: eta identity, %,d terms (about 3 decimal places)%n",
                TERM_COUNT);
        System.out.println();
        System.out.println("Numerical experimentation only. Nothing here is a proof.");
        System.out.println();
        System.out.printf("%8s  %12s  %s%n", "t", "|zeta|", "");
        System.out.println("-".repeat(78));
    }

    private static void scanTheCriticalLine() {
        double previousMagnitude = Double.MAX_VALUE;
        double magnitudeBeforeThat = Double.MAX_VALUE;
        double previousHeight = START_HEIGHT;

        for (double t = START_HEIGHT; t <= END_HEIGHT + 1e-9; t += STEP) {
            double magnitude = magnitudeAt(t);

            boolean isLocalMinimum = previousMagnitude < magnitudeBeforeThat
                    && previousMagnitude < magnitude
                    && previousMagnitude < 0.5;

            System.out.printf("%8.2f  %12.6f  %s%s%n",
                    t, magnitude, bar(magnitude), isLocalMinimum ? "" : "");

            if (isLocalMinimum) {
                System.out.printf("%8s  %12s  <== candidate zero near t = %.2f%n",
                        "", "", previousHeight);
            }

            magnitudeBeforeThat = previousMagnitude;
            previousMagnitude = magnitude;
            previousHeight = t;
        }
    }

    private static void compareAgainstReferenceValues() {
        System.out.println();
        System.out.println("Evaluation at published reference zero heights");
        System.out.println("-".repeat(78));
        System.out.printf("%24s  %14s  %s%n", "reference t", "|zeta|", "interpretation");

        for (double t = 0; t < REFERENCE_ZEROS.length; t++) {
            double height = REFERENCE_ZEROS[(int) t];
            double magnitude = magnitudeAt(height);
            System.out.printf("%24.15f  %14.8f  %s%n",
                    height, magnitude,
                    magnitude < 0.01 ? "consistent with a zero" : "larger than expected");
        }

        System.out.println();
        System.out.println("The magnitudes above are limited by the evaluator, not by the");
        System.out.println("mathematics: the eta series error on this line falls off like");
        System.out.printf("1/sqrt(N), so %,d terms buys roughly %.0e. Reducing that is the%n",
                TERM_COUNT, 1.0 / Math.sqrt(TERM_COUNT));
        System.out.println("subject of the next step: convergence acceleration.");
        System.out.println();
    }

    private static double magnitudeAt(double height) {
        return ZetaFunction
                .evaluate(Complex.of(CRITICAL_LINE, height), TERM_COUNT)
                .magnitude();
    }

    private static String bar(double magnitude) {
        int length = (int) Math.round(Math.min(magnitude * BAR_SCALE, BAR_WIDTH));
        return "#".repeat(Math.max(length, 0));
    }
}
