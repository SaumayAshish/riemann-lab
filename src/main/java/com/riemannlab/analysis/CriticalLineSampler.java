package com.riemannlab.analysis;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.ZetaEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Samples {@code |zeta(1/2 + it)|} at heights along the critical line.
 *
 * <p>Every point here has real part exactly {@code 1/2}, which is inside the
 * eta series' domain - no functional-equation reflection is needed, so
 * whichever evaluator is supplied is asked directly for each point.</p>
 *
 * <p>This class does one job: turn heights into magnitudes. It does not
 * decide where zeros are, does not render anything, and does not know this
 * project has a root finder. That separation is deliberate - see the
 * project-level notes on why a plotted dip is evidence to investigate, not a
 * result to report.</p>
 */
public final class CriticalLineSampler {

    private static final double CRITICAL_LINE_REAL_PART = 0.5;

    private final ZetaEvaluator evaluator;

    /**
     * Creates a sampler that evaluates zeta with the given evaluator.
     *
     * @param evaluator the function to sample along the critical line; must not be null
     */
    public CriticalLineSampler(ZetaEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    }

    /**
     * The magnitude of zeta at a single height on the critical line.
     *
     * @param height the imaginary part of the point to sample; must be finite
     * @return the sample at that height
     */
    public CriticalLineSample sampleAt(double height) {
        if (!Double.isFinite(height)) {
            throw new IllegalArgumentException("height must be finite, got " + height);
        }

        Complex s = Complex.of(CRITICAL_LINE_REAL_PART, height);
        double magnitude = evaluator.evaluate(s).value().magnitude();
        return new CriticalLineSample(height, magnitude);
    }

    /**
     * {@code sampleCount} evenly spaced samples from {@code minHeight} to
     * {@code maxHeight}, inclusive of both ends.
     *
     * @param minHeight the smallest height to sample; must be less than
     *                  {@code maxHeight}
     * @param maxHeight the largest height to sample; must exceed
     *                  {@code minHeight}
     * @param sampleCount the number of samples to take; must be at least 2
     * @return the samples, in increasing height order
     */
    public List<CriticalLineSample> sample(double minHeight, double maxHeight, int sampleCount) {
        if (!(maxHeight > minHeight)) {
            throw new IllegalArgumentException(
                    "maxHeight must exceed minHeight, got [" + minHeight + ", " + maxHeight + "]");
        }
        if (sampleCount < 2) {
            throw new IllegalArgumentException(
                    "sampleCount must be at least 2, got " + sampleCount);
        }

        List<CriticalLineSample> samples = new ArrayList<>(sampleCount);
        double step = (maxHeight - minHeight) / (sampleCount - 1);

        for (int i = 0; i < sampleCount; i++) {
            double height = (i == sampleCount - 1) ? maxHeight : minHeight + i * step;
            samples.add(sampleAt(height));
        }

        return samples;
    }
}