package com.riemannlab.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ZetaAccuracyReportTest {

    private static final double EPSILON = 1e-12;

    private static ZetaAgreementSample sampleWithDifference(
            double directReal, double directImaginary, double absoluteDifference) {
        Complex direct = Complex.of(directReal, directImaginary);
        Complex reflected = Complex.of(directReal - absoluteDifference, directImaginary);
        return new ZetaAgreementSample(Complex.of(0.3, 5.0), direct, reflected);
    }

    @Nested
    class Validation {

        @Test
        void rejectsNullSamples() {
            assertThrows(NullPointerException.class, () -> new ZetaAccuracyReport(null));
        }

        @Test
        void rejectsEmptySamples() {
            assertThrows(IllegalArgumentException.class, () -> new ZetaAccuracyReport(List.of()));
        }

        @Test
        void copiesDefensively() {
            List<ZetaAgreementSample> mutable = new ArrayList<>();
            mutable.add(sampleWithDifference(3.0, 4.0, 0.01));

            ZetaAccuracyReport report = new ZetaAccuracyReport(mutable);
            mutable.clear();

            assertEquals(1, report.count());
        }
    }

    @Nested
    class Aggregation {

        @Test
        void worstAbsoluteDifferenceIsTheLargestAcrossSamples() {
            List<ZetaAgreementSample> samples = List.of(
                    sampleWithDifference(3.0, 4.0, 0.001),
                    sampleWithDifference(1.0, 1.0, 0.05),
                    sampleWithDifference(2.0, 2.0, 0.01));

            ZetaAccuracyReport report = new ZetaAccuracyReport(samples);

            assertEquals(0.05, report.worstAbsoluteDifference(), EPSILON);
        }

        @Test
        void meanAbsoluteDifferenceAveragesAllSamples() {
            List<ZetaAgreementSample> samples = List.of(
                    sampleWithDifference(3.0, 4.0, 0.01),
                    sampleWithDifference(1.0, 1.0, 0.03));

            ZetaAccuracyReport report = new ZetaAccuracyReport(samples);

            assertEquals(0.02, report.meanAbsoluteDifference(), EPSILON);
        }

        @Test
        void worstRelativeDifferenceIgnoresNonFiniteSamples() {
            ZetaAgreementSample zeroReference = new ZetaAgreementSample(
                    Complex.of(0.5, 14.134725141734693), Complex.of(0.0, 0.0), Complex.of(1e-9, 0.0));
            ZetaAgreementSample normal = sampleWithDifference(3.0, 4.0, 0.05);

            ZetaAccuracyReport report = new ZetaAccuracyReport(List.of(zeroReference, normal));

            assertEquals(normal.relativeDifference(), report.worstRelativeDifference(), EPSILON);
        }

        @Test
        void worstRelativeDifferenceIsNaNWhenNoSampleHasADefinedRelativeDifference() {
            ZetaAgreementSample onlyZeroReference = new ZetaAgreementSample(
                    Complex.of(0.5, 14.134725141734693), Complex.of(0.0, 0.0), Complex.of(1e-9, 0.0));

            ZetaAccuracyReport report = new ZetaAccuracyReport(List.of(onlyZeroReference));

            assertTrue(Double.isNaN(report.worstRelativeDifference()));
        }

        @Test
        void countMatchesNumberOfSamples() {
            List<ZetaAgreementSample> samples = List.of(
                    sampleWithDifference(3.0, 4.0, 0.01),
                    sampleWithDifference(1.0, 1.0, 0.02),
                    sampleWithDifference(2.0, 2.0, 0.03));

            ZetaAccuracyReport report = new ZetaAccuracyReport(samples);

            assertEquals(3, report.count());
        }
    }
}