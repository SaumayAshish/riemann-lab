package com.riemannlab.validation;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.ZetaEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs the same set of points through two zeta evaluators and pairs up the
 * results, ready for {@link ZetaAccuracyReport} to aggregate.
 *
 * <p>This class only measures; it does not judge. Deciding what counts as
 * an acceptable disagreement belongs to whoever reads the report.</p>
 */
public final class ZetaAccuracyChecker {

    private ZetaAccuracyChecker() {
        throw new AssertionError("ZetaAccuracyChecker is a utility class and must not be instantiated");
    }

    /**
     * Evaluates both evaluators at every given point and pairs up their results.
     *
     * @param direct the evaluator computing zeta(s) directly
     * @param reflected the evaluator computing zeta(s) via the functional equation
     * @param points the points to evaluate both evaluators at
     * @return one agreement sample per point
     */
    public static List<ZetaAgreementSample> check(
            ZetaEvaluator direct, ZetaEvaluator reflected, List<Complex> points) {
        Objects.requireNonNull(direct, "direct must not be null");
        Objects.requireNonNull(reflected, "reflected must not be null");
        Objects.requireNonNull(points, "points must not be null");
        if (points.isEmpty()) {
            throw new IllegalArgumentException("points must not be empty");
        }

        List<ZetaAgreementSample> samples = new ArrayList<>(points.size());
        for (Complex point : points) {
            Objects.requireNonNull(point, "points must not contain null");
            Complex directValue = direct.evaluate(point).value();
            Complex reflectedValue = reflected.evaluate(point).value();
            samples.add(new ZetaAgreementSample(point, directValue, reflectedValue));
        }
        return samples;
    }
}