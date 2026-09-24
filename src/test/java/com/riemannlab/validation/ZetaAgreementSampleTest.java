package com.riemannlab.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ZetaAgreementSampleTest {

    private static final double EPSILON = 1e-12;

    @Nested
    class Validation {

        @Test
        void rejectsNullPoint() {
            assertThrows(NullPointerException.class,
                    () -> new ZetaAgreementSample(null, Complex.ONE, Complex.ONE));
        }

        @Test
        void rejectsNullDirectValue() {
            assertThrows(NullPointerException.class,
                    () -> new ZetaAgreementSample(Complex.ONE, null, Complex.ONE));
        }

        @Test
        void rejectsNullReflectedValue() {
            assertThrows(NullPointerException.class,
                    () -> new ZetaAgreementSample(Complex.ONE, Complex.ONE, null));
        }
    }

    @Nested
    class Accessors {

        @Test
        void absoluteDifferenceIsMagnitudeOfDifference() {
            ZetaAgreementSample sample = new ZetaAgreementSample(
                    Complex.of(0.25, 3.0), Complex.of(3.0, 4.0), Complex.of(0.0, 4.0));

            assertEquals(3.0, sample.absoluteDifference(), EPSILON);
        }

        @Test
        void absoluteDifferenceIsZeroWhenValuesAgreeExactly() {
            Complex value = Complex.of(1.5, -2.5);
            ZetaAgreementSample sample = new ZetaAgreementSample(Complex.of(0.3, 5.0), value, value);

            assertEquals(0.0, sample.absoluteDifference(), EPSILON);
        }

        @Test
        void relativeDifferenceIsAbsoluteDifferenceOverDirectMagnitude() {
            ZetaAgreementSample sample = new ZetaAgreementSample(
                    Complex.of(0.25, 3.0), Complex.of(3.0, 4.0), Complex.of(0.0, 4.0));

            // |direct| = 5, absolute difference = 3, so relative = 3/5
            assertEquals(0.6, sample.relativeDifference(), EPSILON);
        }

        @Test
        void relativeDifferenceIsNaNWhenDirectValueIsZero() {
            ZetaAgreementSample sample = new ZetaAgreementSample(
                    Complex.of(0.5, 14.134725141734693), Complex.of(0.0, 0.0), Complex.of(1e-10, 0.0));

            assertTrue(Double.isNaN(sample.relativeDifference()));
        }
    }
}