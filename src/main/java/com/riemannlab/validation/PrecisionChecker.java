package com.riemannlab.validation;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.ZetaEvaluator;
import com.riemannlab.zeta.ZetaResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs an evaluator against points with a known exact answer and records
 * how its self-reported error bound compared to the error it actually
 * made.
 */
public final class PrecisionChecker {

    /**
     * zeta at the negative odd integers, where the functional equation
     * gives an exact rational value: -B(n+1)/(n+1), the same values
     * ContinuationDemo checks by hand.
     */
    public static final List<KnownZetaValue> NEGATIVE_ODD_INTEGER_REFERENCES = List.of(
            new KnownZetaValue(-1.0, -1.0 / 12.0),
            new KnownZetaValue(-3.0, 1.0 / 120.0),
            new KnownZetaValue(-5.0, -1.0 / 252.0),
            new KnownZetaValue(-7.0, 1.0 / 240.0),
            new KnownZetaValue(-9.0, -1.0 / 132.0),
            new KnownZetaValue(-11.0, 691.0 / 32760.0));

    private PrecisionChecker() {
        throw new AssertionError("PrecisionChecker is a utility class and must not be instantiated");
    }

    /**
     * Evaluates the given evaluator at every reference point and pairs each
     * result with the known exact value and the evaluator's own claimed error.
     *
     * @param evaluator the evaluator under test
     * @param references the points with known exact values to test against
     * @return one sample per reference point
     */
    public static List<PrecisionSample> check(ZetaEvaluator evaluator, List<KnownZetaValue> references) {
        Objects.requireNonNull(evaluator, "evaluator must not be null");
        Objects.requireNonNull(references, "references must not be null");
        if (references.isEmpty()) {
            throw new IllegalArgumentException("references must not be empty");
        }

        List<PrecisionSample> samples = new ArrayList<>(references.size());
        for (KnownZetaValue reference : references) {
            Objects.requireNonNull(reference, "references must not contain null");
            ZetaResult result = evaluator.evaluate(Complex.ofReal(reference.s()));
            samples.add(new PrecisionSample(
                    reference.s(), result.value(), reference.exactValue(), result.estimatedErrorBound()));
        }
        return samples;
    }
}
