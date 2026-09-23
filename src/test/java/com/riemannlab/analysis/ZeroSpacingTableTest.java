package com.riemannlab.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.validation.KnownZeroCatalog;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ZeroSpacingTableTest {

    @Nested
    @DisplayName("Building the table")
    class Building {

        @Test
        @DisplayName("The first entry has no spacing")
        void firstEntryHasNoSpacing() {
            List<ZeroSpacing> table = ZeroSpacingTable.compute(List.of(14.13, 21.02, 25.01));

            assertTrue(table.get(0).isFirst());
        }

        @Test
        @DisplayName("Indices are one-based and count up in order")
        void indicesAreOneBased() {
            List<ZeroSpacing> table = ZeroSpacingTable.compute(List.of(14.13, 21.02, 25.01));

            assertEquals(1, table.get(0).index());
            assertEquals(2, table.get(1).index());
            assertEquals(3, table.get(2).index());
        }

        @Test
        @DisplayName("Spacing is the difference from the previous height")
        void spacingIsTheDifference() {
            List<ZeroSpacing> table = ZeroSpacingTable.compute(List.of(14.13, 21.02, 25.01));

            assertEquals(21.02 - 14.13, table.get(1).spacingFromPrevious(), 1e-9);
            assertEquals(25.01 - 21.02, table.get(2).spacingFromPrevious(), 1e-9);
        }

        @Test
        @DisplayName("A single-height list produces one entry with no spacing")
        void singleHeightIsAllowed() {
            List<ZeroSpacing> table = ZeroSpacingTable.compute(List.of(14.13));

            assertEquals(1, table.size());
            assertTrue(table.get(0).isFirst());
        }

        @Test
        @DisplayName("Real catalogued heights produce the expected first gap")
        void matchesTheKnownFirstGap() {
            List<Double> heights = Arrays.stream(KnownZeroCatalog.firstElevenHeights())
                    .boxed()
                    .toList();

            List<ZeroSpacing> table = ZeroSpacingTable.compute(heights);

            // gamma_2 - gamma_1 = 21.022039638771555 - 14.134725141734693
            assertEquals(6.887314497036862, table.get(1).spacingFromPrevious(), 1e-9);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("A null list is rejected")
        void rejectsNullList() {
            assertThrows(NullPointerException.class, () -> ZeroSpacingTable.compute(null));
        }

        @Test
        @DisplayName("An empty list is rejected")
        void rejectsEmptyList() {
            assertThrows(IllegalArgumentException.class,
                    () -> ZeroSpacingTable.compute(List.of()));
        }

        @Test
        @DisplayName("Heights that do not strictly increase are rejected")
        void rejectsNonAscendingHeights() {
            assertThrows(IllegalArgumentException.class,
                    () -> ZeroSpacingTable.compute(List.of(14.13, 14.13, 25.01)));
            assertThrows(IllegalArgumentException.class,
                    () -> ZeroSpacingTable.compute(List.of(21.02, 14.13, 25.01)));
        }
    }
}