package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;

/**
 * Static convenience entry point for evaluating zeta with sensible defaults.
 *
 * <p>This class holds no numerical logic. It exists so that callers who want
 * "just evaluate zeta here" do not have to construct an evaluator, while
 * callers who need to choose a strategy, vary its configuration or read its
 * diagnostics work with {@link ZetaEvaluator} directly.</p>
 *
 * <p>Anything that needs to be substitutable - the zero search, tests, the
 * user interface - should depend on the interface rather than on this class.
 * A static facade cannot be swapped out, which is exactly why it is confined
 * to convenience.</p>
 */
public final class ZetaFunction {

    private static final ZetaEvaluator DEFAULT_EVALUATOR = new AcceleratedEtaEvaluator();

    private ZetaFunction() {
        throw new AssertionError("ZetaFunction is a utility class and must not be instantiated");
    }

    /**
     * Evaluates zeta at {@code s} using the default evaluator.
     *
     * @param s the point at which to evaluate zeta
     * @return the value of zeta at {@code s}
     * @throws IllegalArgumentException if {@code s} is outside the supported domain
     * @throws ArithmeticException      if {@code s} is a singularity
     */
    public static Complex evaluate(Complex s) {
        return DEFAULT_EVALUATOR.valueAt(s);
    }

    /**
     * Evaluates zeta at {@code s}, returning the diagnostics alongside the value.
     *
     * @param s the point at which to evaluate zeta
     * @return the value with its term count and error bound
     */
    public static ZetaResult evaluateWithDiagnostics(Complex s) {
        return DEFAULT_EVALUATOR.evaluate(s);
    }

    /**
     * The shared default evaluator, for callers that want to inspect or reuse it.
     *
     * <p>Safe to share: evaluators are immutable and thread-safe.</p>
     *
     * @return the default evaluator instance
     */
    public static ZetaEvaluator defaultEvaluator() {
        return DEFAULT_EVALUATOR;
    }
}