package com.riemannlab.app.cli;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.numeric.ComplexRootFinder;
import com.riemannlab.core.numeric.NewtonRootFinder;
import com.riemannlab.core.numeric.RootFindingResult;
import com.riemannlab.core.numeric.SecantRootFinder;
import java.util.function.Function;

/**
 * Measures how the two root finders behave on problems whose answers are known
 * exactly, before either is pointed at the zeta function.
 *
 * <p>Three things are shown: the iterate-by-iterate collapse of the error, the
 * convergence order estimated from that collapse, and the cost in function
 * evaluations rather than iterations. The last of those is what decides which
 * method to use on zeta, where a single evaluation runs about forty complex
 * powers.</p>
 *
 * <p>Console output by design.</p>
 */
public final class RootFindingDemo {

    private static final double SQRT_TWO = 1.4142135623730951;

    /** {@code z^2 - 2}, with a real root at sqrt(2). */
    private static final Function<Complex, Complex> Z_SQUARED_MINUS_TWO =
            z -> z.multiply(z).subtract(Complex.ofReal(2.0));

    /** {@code z^3 - 1}, whose primitive roots are off the real axis. */
    private static final Function<Complex, Complex> Z_CUBED_MINUS_ONE =
            z -> z.multiply(z).multiply(z).subtract(Complex.ONE);

    private static final ComplexRootFinder NEWTON = new NewtonRootFinder();
    private static final ComplexRootFinder SECANT = new SecantRootFinder();

    private RootFindingDemo() {
        throw new AssertionError("RootFindingDemo is an entry point and must not be instantiated");
    }
    /** Demonstrates root finding for zeta. */
    public static void main(String[] args) {
        System.out.println();
        System.out.println("RiemannLab - root finder behaviour on known problems");
        System.out.println("=".repeat(84));
        System.out.println("Verified on polynomials first. Nothing here involves zeta yet.");

        showErrorCollapse();
        showComplexRoot();
        showEvaluationCost();
    }

    private static void showErrorCollapse() {
        section("Solving z^2 - 2 = 0 from a deliberately poor guess of 3.0");
        System.out.printf("target: sqrt(2) = %.16f%n", SQRT_TWO);

        printIterates("Newton-Raphson", NEWTON.findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0)));
        printIterates("Secant", SECANT.findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0)));

        System.out.println();
        System.out.println("The order column is log(e_next / e_now) / log(e_now / e_prev). It");
        System.out.println("settles near 2 for Newton and near 1.618 - the golden ratio - for");
        System.out.println("the secant method, then becomes meaningless once the error reaches");
        System.out.println("machine precision and is measuring rounding rather than the method.");
    }

    private static void printIterates(String label, RootFindingResult result) {
        System.out.println();
        System.out.printf("%s: %d iterations, terminated on %s%n",
                label, result.iterations(), result.termination());
        System.out.printf("%6s  %24s  %14s  %10s%n", "n", "iterate", "error", "order");
        System.out.println("-".repeat(84));

        for (int n = 0; n < result.iterates().size(); n++) {
            double error = Math.abs(result.iterates().get(n).real() - SQRT_TWO);
            String order = "";

            if (n >= 2) {
                double previous = Math.abs(result.iterates().get(n - 2).real() - SQRT_TWO);
                double current = Math.abs(result.iterates().get(n - 1).real() - SQRT_TWO);

                if (error > 1e-16 && current > 1e-16 && previous > current && current > error) {
                    order = String.format("%.3f",
                            Math.log(error / current) / Math.log(current / previous));
                }
            }

            System.out.printf("%6d  %24.16f  %14.3e  %10s%n",
                    n, result.iterates().get(n).real(), error, order);
        }
    }

    private static void showComplexRoot() {
        section("Solving z^3 - 1 = 0, whose interesting roots are not on the real axis");

        Complex guess = Complex.of(-0.4, 0.85);
        Complex expected = Complex.of(-0.5, Math.sqrt(3.0) / 2.0);

        System.out.printf("guess:    %s%n", guess);
        System.out.printf("expected: %s   (a primitive cube root of unity)%n", expected);
        System.out.println();
        System.out.printf("%-18s %34s %10s %12s%n",
                "method", "root found", "iters", "residual");
        System.out.println("-".repeat(84));

        for (ComplexRootFinder finder : new ComplexRootFinder[] {NEWTON, SECANT}) {
            RootFindingResult result = finder.findRoot(Z_CUBED_MINUS_ONE, guess);
            System.out.printf("%-18s %34s %10d %12.3e%n",
                    finder.name(), result.root(), result.iterations(),
                    result.residualMagnitude());
        }

        System.out.println();
        System.out.println("Nothing in either algorithm was changed to handle complex numbers.");
        System.out.println("The formulas are identical; only the arithmetic underneath differs.");
        System.out.println("That is why the zeta search can roam the whole plane rather than");
        System.out.println("being pinned to the critical line - which would assume the answer.");
    }

    private static void showEvaluationCost() {
        section("Cost measured in function evaluations, not iterations");

        System.out.printf("%-18s %12s %14s %16s %14s%n",
                "method", "iterations", "evaluations", "per iteration", "digits/eval");
        System.out.println("-".repeat(84));

        for (ComplexRootFinder finder : new ComplexRootFinder[] {NEWTON, SECANT}) {
            int[] callCount = {0};
            Function<Complex, Complex> counted = z -> {
                callCount[0]++;
                return Z_SQUARED_MINUS_TWO.apply(z);
            };

            RootFindingResult result = finder.findRoot(counted, Complex.ofReal(3.0));

            double digitsGained = -Math.log10(Math.max(result.residualMagnitude(), 1e-17))
                    + Math.log10(7.0);

            System.out.printf("%-18s %12d %14d %16.2f %14.3f%n",
                    finder.name(),
                    result.iterations(),
                    callCount[0],
                    (double) callCount[0] / result.iterations(),
                    digitsGained / callCount[0]);
        }

        System.out.println();
        System.out.println("Newton wins on iterations and loses on evaluations. Approximating");
        System.out.println("the derivative costs two extra calls per step, which more than");
        System.out.println("cancels the higher order. When a single evaluation of the target");
        System.out.println("runs forty complex powers, evaluations are the only currency that");
        System.out.println("matters - so the zeta refiner will use the secant method.");
        System.out.println();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(84));
    }
}
