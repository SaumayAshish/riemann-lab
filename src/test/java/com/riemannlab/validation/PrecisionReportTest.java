package com.riemannlab.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PrecisionReportTest {

    private static PrecisionSample sampleWithRatio(double exactValue, double actualError, double claimedErrorBound) {
        Complex computed = Complex.ofReal(exactValue + actualError);
        return new PrecisionSample(-1.0, computed, exactValue, claimedErrorBound);
    }

    @Nested
    class Validation {

        @Test
        void rejectsNullSamples() {
            assertThrows(NullPointerException.class, () -> new PrecisionReport(null));
        }

        @Test
        void rejectsEmptySamples() {
            assertThrows(IllegalArgumentException.class, () -> new PrecisionReport(List.of()));
        }

        @Test
        void copiesDefensively() {
            List<PrecisionSample> mutable = new ArrayList<>();
            mutable.add(sampleWithRatio(-1.0 / 12.0, 1e-12, 1e-10));

            PrecisionReport report = new PrecisionReport(mutable);
            mutable.clear();

            assertEquals(1, report.count());
        }
    }

    @Nested
    class Aggregation {

        @Test
        void worstErrorRatioIsTheLargestAcrossSamples() {
            List<PrecisionSample> samples = List.of(
                    sampleWithRatio(-1.0 / 12.0, 1e-4, 1e-3),   // ratio 0.1
                    sampleWithRatio(1.0 / 120.0, 5e-3, 1e-3),   // ratio 5.0
                    sampleWithRatio(-1.0 / 252.0, 2e-4, 1e-3)); // ratio 0.2

            PrecisionReport report = new PrecisionReport(samples);

            assertEquals(5.0, report.worstErrorRatio(), 1e-12);
        }

        @Test
        void countExceedingClaimedBoundCountsOnlyViolations() {
            List<PrecisionSample> samples = List.of(
                    sampleWithRatio(-1.0 / 12.0, 1e-4, 1e-3),   // within bound
                    sampleWithRatio(1.0 / 120.0, 5e-3, 1e-3),   // exceeds bound
                    sampleWithRatio(-1.0 / 252.0, 2e-3, 1e-3)); // exceeds bound

            PrecisionReport report = new PrecisionReport(samples);

            assertEquals(2, report.countExceedingClaimedBound());
        }

        @Test
        void allWithinClaimedBoundIsTrueWhenNothingExceeds() {
            List<PrecisionSample> samples = List.of(
                    sampleWithRatio(-1.0 / 12.0, 1e-4, 1e-3),
                    sampleWithRatio(1.0 / 120.0, 2e-4, 1e-3));

            PrecisionReport report = new PrecisionReport(samples);

            assertTrue(report.allWithinClaimedBound());
        }

        @Test
        void allWithinClaimedBoundIsFalseWhenAnySampleExceeds() {
            List<PrecisionSample> samples = List.of(
                    sampleWithRatio(-1.0 / 12.0, 1e-4, 1e-3),
                    sampleWithRatio(1.0 / 120.0, 5e-3, 1e-3));

            PrecisionReport report = new PrecisionReport(samples);

            assertFalse(report.allWithinClaimedBound());
        }

        @Test
        void countMatchesNumberOfSamples() {
            List<PrecisionSample> samples = List.of(
                    sampleWithRatio(-1.0 / 12.0, 1e-4, 1e-3),
                    sampleWithRatio(1.0 / 120.0, 2e-4, 1e-3),
                    sampleWithRatio(-1.0 / 252.0, 3e-4, 1e-3));

            PrecisionReport report = new PrecisionReport(samples);

            assertEquals(3, report.count());
        }
    }
}