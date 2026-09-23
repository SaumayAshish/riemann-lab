package com.riemannlab.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CriticalLineSampleTest {

    @Nested
    @DisplayName("A valid sample")
    class Accessors {

        @Test
        @DisplayName("Stores the height and magnitude it was given")
        void storesItsValues() {
            CriticalLineSample sample = new CriticalLineSample(14.13, 0.0002);

            assertEquals(14.13, sample.height(), 0.0);
            assertEquals(0.0002, sample.magnitude(), 0.0);
        }

        @Test
        @DisplayName("A magnitude of exactly zero is allowed")
        void zeroMagnitudeIsAllowed() {
            CriticalLineSample sample = new CriticalLineSample(0.0, 0.0);

            assertEquals(0.0, sample.magnitude(), 0.0);
        }

        @Test
        @DisplayName("A negative height is allowed - it is a position, not a size")
        void negativeHeightIsAllowed() {
            CriticalLineSample sample = new CriticalLineSample(-5.0, 1.0);

            assertEquals(-5.0, sample.height(), 0.0);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("A non-finite height is rejected")
        void rejectsNonFiniteHeight() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CriticalLineSample(Double.NaN, 1.0));
            assertThrows(IllegalArgumentException.class,
                    () -> new CriticalLineSample(Double.POSITIVE_INFINITY, 1.0));
        }

        @Test
        @DisplayName("A negative magnitude is rejected - a distance cannot be negative")
        void rejectsNegativeMagnitude() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CriticalLineSample(1.0, -0.1));
        }

        @Test
        @DisplayName("A non-finite magnitude is rejected")
        void rejectsNonFiniteMagnitude() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CriticalLineSample(1.0, Double.NaN));
            assertThrows(IllegalArgumentException.class,
                    () -> new CriticalLineSample(1.0, Double.POSITIVE_INFINITY));
        }
    }
}