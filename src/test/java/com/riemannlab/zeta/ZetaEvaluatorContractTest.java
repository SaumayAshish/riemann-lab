package com.riemannlab.zeta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Runs the same contract against every {@link ZetaEvaluator} implementation.
 *
 * <p>This is a Liskov substitution test in executable form: if an
 * implementation cannot pass these, callers cannot safely be handed it through
 * the interface. Notice that the accuracy assertions are phrased against each
 * result's own {@code estimatedErrorBound} rather than a fixed tolerance -
 * that is what lets a fast evaluator and a slow one be held to one standard
 * without pretending they are equally accurate.</p>
 *
 * <p>{@code DirichletSeries} is deliberately absent. It is only valid for
 * {@code Re(s) > 1} and would fail the critical-line cases, which is precisely
 * why it does not implement this interface.</p>
 */
class ZetaEvaluatorContractTest {

    private static final double ZETA_AT_TWO = Math.PI * Math.PI / 6.0;
    private static final double ZETA_AT_THREE = 1.2020569031595943;
    private static final double ZETA_AT_HALF = -1.4603545088095868;
    private static final double FIRST_ZERO = 14.134725141734693;

    /** Every implementation the interface promises to support. */
    static Stream<ZetaEvaluator> evaluators() {
        return Stream.of(
                new AcceleratedEtaEvaluator(),
                new NaiveEtaEvaluator(20_000));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("zeta(2) is correct to within the evaluator's own stated error bound")
    void zetaAtTwoIsWithinItsStatedBound(ZetaEvaluator evaluator) {
        ZetaResult result = evaluator.evaluate(Complex.ofReal(2));
        double error = Math.abs(ZETA_AT_TWO - result.value().real());

        assertTrue(error <= result.estimatedErrorBound(),
                evaluator.name() + ": error " + error + " exceeded its own claimed bound "
                        + result.estimatedErrorBound());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("zeta(3) is correct to within the evaluator's own stated error bound")
    void zetaAtThreeIsWithinItsStatedBound(ZetaEvaluator evaluator) {
        ZetaResult result = evaluator.evaluate(Complex.ofReal(3));
        double error = Math.abs(ZETA_AT_THREE - result.value().real());

        assertTrue(error <= result.estimatedErrorBound(),
                evaluator.name() + ": error " + error + " exceeded bound "
                        + result.estimatedErrorBound());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("zeta(1/2) is correct to within the evaluator's own stated error bound")
    void zetaAtHalfIsWithinItsStatedBound(ZetaEvaluator evaluator) {
        ZetaResult result = evaluator.evaluate(Complex.ofReal(0.5));
        double error = Math.abs(ZETA_AT_HALF - result.value().real());

        assertTrue(error <= result.estimatedErrorBound(),
                evaluator.name() + ": error " + error + " exceeded bound "
                        + result.estimatedErrorBound());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("A known zero is indistinguishable from zero at this evaluator's precision")
    void knownZeroIsIndistinguishableFromZero(ZetaEvaluator evaluator) {
        ZetaResult result = evaluator.evaluate(Complex.of(0.5, FIRST_ZERO));

        assertTrue(result.isIndistinguishableFromZero(),
                evaluator.name() + ": |zeta| = " + result.value().magnitude()
                        + " against bound " + result.estimatedErrorBound()
                        + " - a known zero must not be distinguishable from zero");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("A point away from any zero is clearly distinguishable from zero")
    void nonZeroPointIsDistinguishable(ZetaEvaluator evaluator) {
        ZetaResult result = evaluator.evaluate(Complex.of(0.5, 17.75));

        assertTrue(result.value().magnitude() > 1.0,
                evaluator.name() + ": |zeta(0.5+17.75i)| was " + result.value().magnitude());
        assertTrue(!result.isIndistinguishableFromZero(),
                evaluator.name() + " must not mistake a large value for a zero");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("The pole at s = 1 is rejected")
    void rejectsThePole(ZetaEvaluator evaluator) {
        assertThrows(ArithmeticException.class, () -> evaluator.evaluate(Complex.ONE));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("Arguments outside the eta domain, Re(s) <= 0, are rejected")
    void rejectsArgumentsOutsideTheEtaDomain(ZetaEvaluator evaluator) {
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(Complex.of(-1, 3)));
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(Complex.of(0, 5)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("Every result reports a usable term count and a non-negative bound")
    void resultsCarryHonestDiagnostics(ZetaEvaluator evaluator) {
        ZetaResult result = evaluator.evaluate(Complex.of(0.5, 12.0));

        assertTrue(result.termsUsed() >= 1,
                evaluator.name() + " reported termsUsed = " + result.termsUsed());
        assertTrue(result.estimatedErrorBound() >= 0.0,
                evaluator.name() + " reported a negative error bound");
        assertTrue(Double.isFinite(result.value().real())
                        && Double.isFinite(result.value().imaginary()),
                evaluator.name() + " returned a non-finite value");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("valueAt agrees with the value inside the full result")
    void valueAtIsConsistentWithEvaluate(ZetaEvaluator evaluator) {
        Complex s = Complex.of(0.5, 8.0);

        Complex shorthand = evaluator.valueAt(s);
        Complex full = evaluator.evaluate(s).value();

        assertEquals(full.real(), shorthand.real(), 0.0);
        assertEquals(full.imaginary(), shorthand.imaginary(), 0.0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("Every evaluator identifies itself")
    void everyEvaluatorHasAName(ZetaEvaluator evaluator) {
        assertNotNull(evaluator.name());
        assertTrue(!evaluator.name().isBlank(), "name must not be blank");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evaluators")
    @DisplayName("Evaluation is deterministic: the same input gives the same output")
    void evaluationIsDeterministic(ZetaEvaluator evaluator) {
        Complex s = Complex.of(0.75, 6.5);

        Complex first = evaluator.valueAt(s);
        Complex second = evaluator.valueAt(s);

        assertEquals(first.real(), second.real(), 0.0);
        assertEquals(first.imaginary(), second.imaginary(), 0.0);
    }
}
