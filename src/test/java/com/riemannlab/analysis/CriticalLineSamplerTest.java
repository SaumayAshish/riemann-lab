package com.riemannlab.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.zeta.ContinuedZetaEvaluator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the sampler against facts already established elsewhere in this
 * project: the closed-form value of zeta(1/2), and the first non-trivial
 * zero's published height, which earlier steps already refined to within a
 * few units in the last place.
 */
class CriticalLineSamplerTest {

    private static final double FIRST_ZERO_HEIGHT = 14.134725141734693;

    private final CriticalLineSampler sampler =
            new CriticalLineSampler(new ContinuedZetaEvaluator());

    @Nested
    @DisplayName("Sampling a single point")
    class SinglePoint {

        @Test
        @DisplayName("At height zero, the magnitude matches the known value of zeta(1/2)")
        void matchesTheKnownValueAtHeightZero() {
            CriticalLineSample sample = sampler.sampleAt(0.0);

            assertEquals(1.4603545088095868, sample.magnitude(), 1e-9,
                    "zeta(1/2) is a known constant, approximately -1.46035450880958681");
        }

        @Test
        @DisplayName("At the first zero's published height, the magnitude is near zero")
        void isNearZeroAtTheFirstZerosHeight() {
            CriticalLineSample sample = sampler.sampleAt(FIRST_ZERO_HEIGHT);

            assertTrue(sample.magnitude() < 1e-6,
                    "expected a magnitude close to zero at the first zero's height, got "
                            + sample.magnitude());
        }

        @Test
        @DisplayName("Away from any zero, the magnitude is not small")
        void isNotSmallAwayFromAZero() {
            CriticalLineSample sample = sampler.sampleAt(10.0);

            assertTrue(sample.magnitude() > 0.1,
                    "t = 10 sits well clear of the first zero at 14.13, "
                            + "so the magnitude should be an ordinary size, got "
                            + sample.magnitude());
        }

        @Test
        @DisplayName("A non-finite height is rejected")
        void rejectsNonFiniteHeight() {
            assertThrows(IllegalArgumentException.class, () -> sampler.sampleAt(Double.NaN));
        }
    }

    @Nested
    @DisplayName("Sampling a range")
    class Range {

        @Test
        @DisplayName("Produces exactly the requested number of samples")
        void producesTheRequestedCount() {
            List<CriticalLineSample> samples = sampler.sample(0.0, 20.0, 21);

            assertEquals(21, samples.size());
        }

        @Test
        @DisplayName("Samples are evenly spaced")
        void samplesAreEvenlySpaced() {
            List<CriticalLineSample> samples = sampler.sample(0.0, 20.0, 21);

            for (int i = 0; i < samples.size(); i++) {
                assertEquals((double) i, samples.get(i).height(), 1e-9,
                        "sample " + i + " landed at the wrong height");
            }
        }

        @Test
        @DisplayName("The first and last samples land exactly on the requested bounds")
        void endpointsAreExact() {
            List<CriticalLineSample> samples = sampler.sample(1.0, 99.0, 50);

            assertEquals(1.0, samples.get(0).height(), 0.0);
            assertEquals(99.0, samples.get(samples.size() - 1).height(), 0.0,
                    "floating-point step accumulation must not creep the last "
                            + "sample away from the requested bound");
        }

        @Test
        @DisplayName("A range that does not go forward is rejected")
        void rejectsAnEmptyOrReversedRange() {
            assertThrows(IllegalArgumentException.class, () -> sampler.sample(5.0, 5.0, 10));
            assertThrows(IllegalArgumentException.class, () -> sampler.sample(5.0, 1.0, 10));
        }

        @Test
        @DisplayName("Fewer than two samples is rejected - a curve needs at least two points")
        void rejectsTooFewSamples() {
            assertThrows(IllegalArgumentException.class, () -> sampler.sample(0.0, 10.0, 1));
            assertThrows(IllegalArgumentException.class, () -> sampler.sample(0.0, 10.0, 0));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("A null evaluator is rejected")
        void rejectsNullEvaluator() {
            assertThrows(NullPointerException.class, () -> new CriticalLineSampler(null));
        }
    }
}