package com.riemannlab.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ZeroSpacingTest {

    @Nested
    @DisplayName("A valid entry")
    class Accessors {

        @Test
        @DisplayName("Stores the index, height and spacing it was given")
        void storesItsValues() {
            ZeroSpacing entry = new ZeroSpacing(2, 21.02, 6.89);

            assertEquals(2, entry.index());
            assertEquals(21.02, entry.height(), 0.0);
            assertEquals(6.89, entry.spacingFromPrevious(), 0.0);
        }

        @Test
        @DisplayName("A NaN spacing marks the first entry in a table")
        void nanSpacingMeansFirst() {
            ZeroSpacing entry = new ZeroSpacing(1, 14.13, Double.NaN);

            assertTrue(entry.isFirst());
        }

        @Test
        @DisplayName("A real spacing means this is not the first entry")
        void realSpacingMeansNotFirst() {
            ZeroSpacing entry = new ZeroSpacing(2, 21.02, 6.89);

            assertFalse(entry.isFirst());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("An index below one is rejected")
        void rejectsIndexBelowOne() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroSpacing(0, 14.13, Double.NaN));
        }

        @Test
        @DisplayName("A non-finite height is rejected")
        void rejectsNonFiniteHeight() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroSpacing(1, Double.NaN, Double.NaN));
        }

        @Test
        @DisplayName("A zero or negative spacing is rejected - zeros cannot coincide or reverse")
        void rejectsNonPositiveSpacing() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroSpacing(2, 21.02, 0.0));
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroSpacing(2, 21.02, -1.0));
        }

        @Test
        @DisplayName("An infinite spacing is rejected")
        void rejectsInfiniteSpacing() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroSpacing(2, 21.02, Double.POSITIVE_INFINITY));
        }
    }
}