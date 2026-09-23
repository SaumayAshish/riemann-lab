package com.riemannlab.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalDouble;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KnownZeroCatalogTest {

    @Nested
    @DisplayName("The catalogued heights")
    class Heights {

        @Test
        @DisplayName("There are eleven of them")
        void hasElevenHeights() {
            assertEquals(11, KnownZeroCatalog.firstElevenHeights().length);
            assertEquals(11, KnownZeroCatalog.count());
        }

        @Test
        @DisplayName("They are in strictly ascending order")
        void areAscending() {
            double[] heights = KnownZeroCatalog.firstElevenHeights();
            for (int i = 1; i < heights.length; i++) {
                assertTrue(heights[i] > heights[i - 1],
                        "entry " + i + " (" + heights[i] + ") is not greater than entry "
                                + (i - 1) + " (" + heights[i - 1] + ")");
            }
        }

        @Test
        @DisplayName("The first and last match the published values")
        void matchPublishedValues() {
            double[] heights = KnownZeroCatalog.firstElevenHeights();
            assertEquals(14.134725141734693, heights[0], 0.0);
            assertEquals(52.970321477714460, heights[10], 0.0);
        }

        @Test
        @DisplayName("The returned array is a defensive copy")
        void returnsADefensiveCopy() {
            double[] first = KnownZeroCatalog.firstElevenHeights();
            first[0] = -1.0;

            double[] second = KnownZeroCatalog.firstElevenHeights();
            assertEquals(14.134725141734693, second[0], 0.0,
                    "mutating a returned array must not affect the catalogue");
        }
    }

    @Nested
    @DisplayName("Looking up a single height")
    class Lookup {

        @Test
        @DisplayName("The first zero's height is present and correct")
        void firstZeroIsPresent() {
            OptionalDouble height = KnownZeroCatalog.heightOf(1);

            assertTrue(height.isPresent());
            assertEquals(14.134725141734693, height.getAsDouble(), 0.0);
        }

        @Test
        @DisplayName("The eleventh zero's height is present and correct")
        void eleventhZeroIsPresent() {
            OptionalDouble height = KnownZeroCatalog.heightOf(11);

            assertTrue(height.isPresent());
            assertEquals(52.970321477714460, height.getAsDouble(), 0.0);
        }

        @Test
        @DisplayName("An index outside the catalogue is empty, not an error")
        void outOfRangeIndexIsEmpty() {
            assertFalse(KnownZeroCatalog.heightOf(0).isPresent());
            assertFalse(KnownZeroCatalog.heightOf(12).isPresent());
            assertFalse(KnownZeroCatalog.heightOf(-5).isPresent());
        }
    }
}