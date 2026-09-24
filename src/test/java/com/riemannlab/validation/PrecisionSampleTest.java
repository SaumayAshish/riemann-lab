package com.riemannlab.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PrecisionSampleTest {

    private static final double EPSILON = 1e-12;

    @Nested
    class Validation {

        @Test
        void rejectsNonFiniteS() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PrecisionSample(Double.NaN, Complex.ofReal(-1.0 / 12.0), -1.0 / 12.0, 1e-10));
        }

        @Test
        void rejectsNullComputedValue() {
            assertThrows(NullPointerException.class,
                    () -> new PrecisionSample(-1.0, null, -1.0 / 12.0, 1e-10));
        }

        @Test
        void rejectsNonFiniteExactValue() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PrecisionSample(-1.0, Complex.ofReal(-1.0 / 12.0), Double.NaN, 1e-10));
        }

        @Test
        void rejectsNegativeClaimedErrorBound() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PrecisionSample(-1.0, Complex.ofReal(-1.0 / 12.0), -1.0 / 12.0, -1e-10));
        }

        @Test
        void rejectsNonFiniteClaimedErrorBound() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PrecisionSample(
                            -1.0, Complex.ofReal(-1.0 / 12.0), -1.0 / 12.0, Double.POSITIVE_INFINITY));
        }
    }

    @Nested
    class Accessors {

        @Test
        void actualErrorIsDistanceFromExactValue() {
            PrecisionSample sample = new PrecisionSample(-1.0, Complex.of(3.0, 4.0), 0.0, 10.0);

            // computed = 3+4i, exact = 0 -> magnitude of (3,4) = 5
            assertEquals(5.0, sample.actualError(), EPSILON);
        }

        @Test
        void errorRatioIsActualErrorOverClaimedBound() {
            PrecisionSample sample = new PrecisionSample(-1.0, Complex.ofReal(-0.05), 0.0, 0.1);

            assertEquals(0.5, sample.errorRatio(), EPSILON);
        }

        @Test
        void errorRatioIsZeroWhenBoundAndErrorAreBothZero() {
            PrecisionSample sample = new PrecisionSample(-1.0, Complex.ofReal(-1.0 / 12.0), -1.0 / 12.0, 0.0);

            assertEquals(0.0, sample.errorRatio(), EPSILON);
        }

        @Test
        void errorRatioIsInfiniteWhenBoundIsZeroButErrorIsNot() {
            PrecisionSample sample = new PrecisionSample(-1.0, Complex.ofReal(-0.08), -1.0 / 12.0, 0.0);

            assertEquals(Double.POSITIVE_INFINITY, sample.errorRatio());
        }

        @Test
        void isWithinClaimedBoundWhenErrorDoesNotExceedIt() {
            PrecisionSample sample = new PrecisionSample(-1.0, Complex.ofReal(-1.0 / 12.0), -1.0 / 12.0, 1e-10);

            assertTrue(sample.isWithinClaimedBound());
        }

        @Test
        void isNotWithinClaimedBoundWhenErrorExceedsIt() {
            PrecisionSample sample = new PrecisionSample(-1.0, Complex.ofReal(-0.5), -1.0 / 12.0, 1e-10);

            assertFalse(sample.isWithinClaimedBound());
        }
    }
}