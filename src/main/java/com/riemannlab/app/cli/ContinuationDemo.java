package com.riemannlab.app.cli;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import com.riemannlab.zeta.ZetaResult;

/**
 * Shows what the functional equation buys: zeta on the whole complex plane,
 * the trivial zeros, and the symmetry that forces the non-trivial zeros into
 * mirror pairs.
 *
 * <p>The section worth reading twice is the cross-validation. For
 * {@code 0 < Re(s) < 1/2} the same value is reachable two entirely independent
 * ways - directly by the accelerated eta series, or by reflecting to
 * {@code 1-s} and multiplying by four other factors including a gamma function
 * and a complex sine. Neither route knows the other exists.</p>
 *
 * <p><strong>What this establishes and what it does not.</strong> It shows
 * that the continuation reproduces values known in closed form, that the
 * trivial zeros appear without being told about them, and that reflection
 * about the critical line maps zeros to zeros. That last fact is a theorem,
 * not a measurement. What remains unknown - and what this program cannot
 * touch - is whether every non-trivial zero sits on the line.</p>
 *
 * <p>Console output by design.</p>
 */
public final class ContinuationDemo {

    private static final AcceleratedEtaEvaluator SERIES = new AcceleratedEtaEvaluator();
    private static final ContinuedZetaEvaluator ZETA = new ContinuedZetaEvaluator(SERIES);

    private static final double FIRST_ZERO = 14.134725141734693;

    /**
     * {@code zeta(-n)} for odd {@code n}, which is {@code -B(n+1)/(n+1)} and
     * therefore exactly rational. Written as fractions rather than decimals so
     * the source says where they come from.
     */
    private static final double[][] ODD_NEGATIVE_INTEGERS = {
            {-1.0, -1.0 / 12.0},
            {-3.0, 1.0 / 120.0},
            {-5.0, -1.0 / 252.0},
            {-7.0, 1.0 / 240.0},
            {-9.0, -1.0 / 132.0},
            {-11.0, 691.0 / 32760.0}
    };

    private ContinuationDemo() {
        throw new AssertionError("ContinuationDemo is an entry point and must not be instantiated");
    }
    /**
     * Demonstrates zeta evaluation via analytic continuation.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        printHeader();
        printNegativeIntegers();
        printTrivialZeros();
        printCrossValidation();
        printQuadruple();
        printReach();
        printDisclaimer();
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("RiemannLab - analytic continuation by the functional equation");
        System.out.println("=".repeat(92));
        System.out.println("zeta(s) = 2^s * pi^(s-1) * sin(pi s / 2) * Gamma(1-s) * zeta(1-s)");
        System.out.println();
        System.out.println("Right of Re(s) = 1/2 the eta series is used directly. Left of it the");
        System.out.println("equation above reflects to 1-s, which lands back on the right.");
    }

    private static void printNegativeIntegers() {
        section("Values at the negative odd integers, where the answer is a known fraction");
        System.out.printf("%6s  %24s  %24s  %11s%n", "s", "computed", "exact", "difference");
        System.out.println("-".repeat(92));

        double worst = 0.0;

        for (double[] pair : ODD_NEGATIVE_INTEGERS) {
            ZetaResult result = ZETA.evaluate(Complex.ofReal(pair[0]));
            double difference = Math.abs(result.value().real() - pair[1]);
            worst = Math.max(worst, difference);

            System.out.printf("%6.0f  %24.17f  %24.17f  %11.2e%n",
                    pair[0], result.value().real(), pair[1], difference);
        }

        System.out.println();
        System.out.printf("Largest difference: %.2e%n", worst);
        System.out.println();
        System.out.println("These are rational numbers - -1/12, 1/120, 691/32760 - arrived at");
        System.out.println("through a gamma function, a complex sine and two complex powers.");
        System.out.println("Nothing in the code knows they are rational.");
    }

    private static void printTrivialZeros() {
        section("The trivial zeros, at every negative even integer");
        System.out.printf("%6s  %14s  %14s  %10s  %s%n",
                "s", "|zeta(s)|", "error bound", "ratio", "verdict");
        System.out.println("-".repeat(92));

        for (int n = 2; n <= 14; n += 2) {
            ZetaResult result = ZETA.evaluate(Complex.ofReal(-n));
            double magnitude = result.value().magnitude();
            double bound = result.estimatedErrorBound();

            System.out.printf("%6d  %14.3e  %14.3e  %10.3f  %s%n",
                    -n, magnitude, bound, magnitude / bound,
                    result.isIndistinguishableFromZero()
                            ? "indistinguishable from zero"
                            : "DISTINGUISHABLE - not a zero");
        }

        System.out.println();
        System.out.println("The sine factor is sin(-pi), sin(-2pi), sin(-3pi), ... which is zero");
        System.out.println("at every one of these points, while every other factor is finite and");
        System.out.println("non-zero. So zeta must vanish here. The code was never told that; it");
        System.out.println("falls out of the arithmetic.");
        System.out.println();
        System.out.println("Compare the odd integers above, where the sine is +/-1 and nothing");
        System.out.println("vanishes. That is the whole difference between the two tables.");
    }

    private static void printCrossValidation() {
        section("Two independent routes to the same number, where both are valid");
        System.out.println("For 0 < Re(s) < 1/2 the eta series still converges, so the value can");
        System.out.println("be computed twice - once directly, once by reflecting to 1-s.");
        System.out.println();
        System.out.printf("%18s  %26s  %11s  %11s%n",
                "s", "|zeta(s)|", "difference", "relative");
        System.out.println("-".repeat(92));

        Complex[] samples = {
                Complex.of(0.25, 3.0),
                Complex.of(0.10, 7.0),
                Complex.of(0.30, -5.0),
                Complex.of(0.40, 20.0),
                Complex.of(0.45, 30.0)
        };

        for (Complex s : samples) {
            Complex direct = SERIES.evaluate(s).value();
            Complex continued = ZETA.evaluate(s).value();
            double difference = direct.subtract(continued).magnitude();

            System.out.printf("%18s  %26.16f  %11.2e  %11.2e%n",
                    String.format("%.2f %+.2fi", s.real(), s.imaginary()),
                    direct.magnitude(),
                    difference,
                    difference / direct.magnitude());
        }

        System.out.println();
        System.out.println("An accelerated alternating series on one side; a five-factor product");
        System.out.println("involving Gamma and a complex sine on the other. Agreement to twelve");
        System.out.println("or thirteen digits is not something a broken implementation produces");
        System.out.println("by accident. This is the strongest check in the project.");
    }

    private static void printQuadruple() {
        section("The symmetry the equation forces on the zeros");
        System.out.println("Inside the critical strip every factor except zeta(1-s) is non-zero:");
        System.out.println("the exponentials never vanish, the sine has no even integer to vanish");
        System.out.println("at, and Gamma is never zero anywhere. So zeta(s) = 0 exactly when");
        System.out.println("zeta(1-s) = 0. With conjugate symmetry, a zero drags three companions");
        System.out.println("along: rho, conj(rho), 1-rho, 1-conj(rho).");
        System.out.println();

        Complex rho = Complex.of(0.5, FIRST_ZERO);
        Complex[] quadruple = {
                rho,
                rho.conjugate(),
                Complex.ONE.subtract(rho),
                Complex.ONE.subtract(rho.conjugate())
        };
        String[] labels = {"rho", "conj(rho)", "1 - rho", "1 - conj(rho)"};

        System.out.printf("%16s  %32s  %14s%n", "member", "value", "|zeta|");
        System.out.println("-".repeat(92));

        for (int i = 0; i < quadruple.length; i++) {
            System.out.printf("%16s  %32s  %14.3e%n",
                    labels[i], quadruple[i], ZETA.evaluate(quadruple[i]).value().magnitude());
        }

        System.out.println();
        System.out.println("Four labels, two distinct points. On the critical line reflection and");
        System.out.println("conjugation are the same operation, so the quadruple collapses to a");
        System.out.println("pair.");
        System.out.println();
        System.out.println("A zero anywhere else in the strip would give four genuinely different");
        System.out.println("points. The Riemann Hypothesis is the claim that the collapse always");
        System.out.println("happens - that the four-point case never occurs. Observing the");
        System.out.println("collapse here, at one zero, is not evidence for that claim in any");
        System.out.println("useful sense: a zero on the line is where the collapse happens by");
        System.out.println("definition.");
    }

    private static void printReach() {
        section("How far left the continuation reaches, and what it costs");
        System.out.printf("%18s  %20s  %14s%n", "s", "|zeta(s)|", "error bound");
        System.out.println("-".repeat(92));

        for (double x : new double[] {-2.5, -10.0, -25.0, -50.0, -100.0, -170.0}) {
            Complex s = Complex.of(x, 0.5);
            ZetaResult result = ZETA.evaluate(s);

            System.out.printf("%18s  %20.6e  %14.2e%n",
                    String.format("%.1f + 0.5i", x),
                    result.value().magnitude(),
                    result.estimatedErrorBound());
        }

        System.out.println();
        System.out.println("Zeta grows without limit to the left, and Gamma(1-s) grows with it.");
        System.out.println("Gamma(172) already overflows a double, so the continuation stops near");
        System.out.println("Re(s) = -170 and says so rather than returning NaN. Going further");
        System.out.println("needs extended-precision arithmetic, not a better formula.");
    }

    private static void printDisclaimer() {
        section("What this does and does not establish");
        System.out.println("Established:");
        System.out.println("  - zeta reproduces its known closed-form values at the negative");
        System.out.println("    integers, to within a few units in the last place");
        System.out.println("  - the trivial zeros appear without being hard-coded anywhere");
        System.out.println("  - two independent computational routes agree to ~1e-13 relative");
        System.out.println("  - reflection about the critical line maps zeros to zeros, which is");
        System.out.println("    a consequence of the equation rather than an observation");
        System.out.println();
        System.out.println("NOT established:");
        System.out.println("  - that every non-trivial zero lies on the critical line");
        System.out.println("  - anything at all about zeros this program has not examined");
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
        System.out.println("=".repeat(92));
    }
}