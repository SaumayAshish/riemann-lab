package com.riemannlab.app.cli;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.numeric.AlternatingSeriesAccelerator;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.NaiveEtaEvaluator;
import com.riemannlab.zeta.ZetaResult;

/**
 * Measures what convergence acceleration actually buys, against the raw eta
 * partial sum it replaces.
 *
 * <p>Three measurements: error versus acceleration order at a fixed point,
 * a head-to-head against the raw series at the first known zero, and the
 * order required as a function of height - which is where this evaluator's
 * ceiling comes from.</p>
 *
 * <p>Every evaluator is constructed once, as a static field, so that the
 * configuration lines they log at construction appear before the tables
 * rather than interleaved with them. Constructing an evaluator inside a
 * timed region would also pollute the measurement.</p>
 *
 * <p>Console output by design. This program does not prove anything about
 * the Riemann Hypothesis; it measures the behaviour of an algorithm.</p>
 */
public final class AccelerationDemo {

    private static final double ZETA_AT_TWO = Math.PI * Math.PI / 6.0;
    private static final double FIRST_ZERO = 14.134725141734693;
    private static final int RAW_TERM_COUNT = 20_000;

    /** Acceleration orders sampled in the first experiment. */
    private static final int[] SAMPLED_ORDERS = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26};

    /**
     * Height coefficient used for the fixed-order experiment. The points
     * sampled there lie on the real axis, so no height adjustment applies;
     * the value only has to be positive to satisfy the constructor.
     */
    private static final double NO_HEIGHT_ADJUSTMENT = 0.0001;

    /** The production evaluator, with its default adaptive order policy. */
    private static final AcceleratedEtaEvaluator FAST = new AcceleratedEtaEvaluator();

    /** The unaccelerated evaluator it replaces. */
    private static final NaiveEtaEvaluator RAW = new NaiveEtaEvaluator(RAW_TERM_COUNT);

    /** One evaluator per sampled order, built up front so their logs do not split the table. */
    private static final AcceleratedEtaEvaluator[] FIXED_ORDER = buildFixedOrderEvaluators();

    private AccelerationDemo() {
        throw new AssertionError("AccelerationDemo is an entry point and must not be instantiated");
    }
    /** Demonstrates convergence acceleration for the eta series. */
    public static void main(String[] args) {
        System.out.println();
        System.out.println("RiemannLab - convergence acceleration measurements");
        System.out.println("=".repeat(84));
        System.out.println("Numerical experimentation only. Nothing here is a proof.");

        showErrorVersusOrder();
        showHeadToHeadAtTheFirstZero();
        showOrderRequiredByHeight();
    }

    private static AcceleratedEtaEvaluator[] buildFixedOrderEvaluators() {
        AcceleratedEtaEvaluator[] evaluators = new AcceleratedEtaEvaluator[SAMPLED_ORDERS.length];
        for (int i = 0; i < SAMPLED_ORDERS.length; i++) {
            evaluators[i] = new AcceleratedEtaEvaluator(SAMPLED_ORDERS[i], NO_HEIGHT_ADJUSTMENT);
        }
        return evaluators;
    }

    private static void showErrorVersusOrder() {
        section("Error against acceleration order, evaluating zeta(2) = pi^2/6");
        System.out.printf("%8s  %22s  %14s  %14s  %14s%n",
                "order", "zeta(2)", "actual error", "5.83^-order", "claimed bound");
        System.out.println("-".repeat(84));

        for (int i = 0; i < SAMPLED_ORDERS.length; i++) {
            ZetaResult result = FIXED_ORDER[i].evaluate(Complex.ofReal(2));
            double error = Math.abs(ZETA_AT_TWO - result.value().real());

            System.out.printf("%8d  %22.16f  %14.4e  %14.4e  %14.4e%n",
                    SAMPLED_ORDERS[i],
                    result.value().real(),
                    error,
                    AlternatingSeriesAccelerator.errorBound(SAMPLED_ORDERS[i]),
                    result.estimatedErrorBound());
        }

        System.out.println();
        System.out.println("Each extra term multiplies the error by about 1/5.83, which is");
        System.out.println("0.766 decimal digits per term. The raw series needed ten times the");
        System.out.println("work for one extra digit.");
        System.out.println();
        System.out.println("Compare the last two columns. The theoretical bound keeps falling");
        System.out.println("forever; the evaluator's claimed bound flattens out, because past a");
        System.out.println("point floating-point rounding dominates and further terms buy");
        System.out.println("nothing. Claiming the theoretical figure there would be a lie the");
        System.out.println("zero search would act on.");
    }

    private static void showHeadToHeadAtTheFirstZero() {
        section("Head to head at the first known zero, s = 0.5 + 14.134725141734693i");

        Complex s = Complex.of(0.5, FIRST_ZERO);

        long rawStart = System.nanoTime();
        ZetaResult rawResult = RAW.evaluate(s);
        long rawNanos = System.nanoTime() - rawStart;

        long fastStart = System.nanoTime();
        ZetaResult fastResult = FAST.evaluate(s);
        long fastNanos = System.nanoTime() - fastStart;

        double rawMagnitude = rawResult.value().magnitude();
        double fastMagnitude = fastResult.value().magnitude();

        System.out.printf("%-34s %16s %16s%n", "", "raw eta series", "accelerated");
        System.out.println("-".repeat(84));
        System.out.printf("%-34s %16d %16d%n", "terms evaluated",
                rawResult.termsUsed(), fastResult.termsUsed());
        System.out.printf("%-34s %16.3e %16.3e%n", "|zeta| (true value is 0)",
                rawMagnitude, fastMagnitude);
        System.out.printf("%-34s %16.3e %16.3e%n", "claimed error bound",
                rawResult.estimatedErrorBound(), fastResult.estimatedErrorBound());
        System.out.printf("%-34s %16s %16s%n", "indistinguishable from zero?",
                rawResult.isIndistinguishableFromZero(),
                fastResult.isIndistinguishableFromZero());
        System.out.printf("%-34s %16.3f %16.3f%n", "milliseconds",
                rawNanos / 1e6, fastNanos / 1e6);

        System.out.println();
        System.out.printf("Work reduced by a factor of %.0f.%n",
                (double) rawResult.termsUsed() / fastResult.termsUsed());
        System.out.printf("Error reduced by a factor of %.3e.%n", rawMagnitude / fastMagnitude);
        System.out.println();
        System.out.println("Both evaluators correctly report this point as indistinguishable");
        System.out.println("from zero - but they mean very different things by it. Each judges");
        System.out.println("against its own error bound, which is why the zero search can be");
        System.out.println("handed either one and still behave sensibly.");
    }

    private static void showOrderRequiredByHeight() {
        section("Acceleration order required as height increases");
        System.out.println("The error bound picks up a factor of about e^(pi|t|/2) for complex");
        System.out.println("arguments, so the order must grow with height to compensate.");
        System.out.println();
        System.out.printf("%12s  %12s  %16s  %16s%n",
                "t", "order", "|zeta(0.5+it)|", "claimed bound");
        System.out.println("-".repeat(84));

        for (double t : new double[] {0, 14.134725141734693, 25, 50, 100, 150, 180}) {
            Complex s = Complex.of(0.5, t);
            ZetaResult result = FAST.evaluate(s);

            System.out.printf("%12.4f  %12d  %16.8f  %16.3e%n",
                    t, result.termsUsed(), result.value().magnitude(),
                    result.estimatedErrorBound());
        }

        System.out.println();
        System.out.printf("Beyond |t| of roughly %.0f the required order exceeds what double%n",
                FAST.maxSupportedHeight());
        System.out.println("precision supports, and the evaluator refuses rather than returning");
        System.out.println("a wrong answer. Higher heights need the Riemann-Siegel formula,");
        System.out.println("which is outside the scope of this project.");
        System.out.println();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(84));
    }
}
