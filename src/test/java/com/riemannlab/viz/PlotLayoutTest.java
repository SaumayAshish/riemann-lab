package com.riemannlab.viz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlotLayoutTest {

    private static final PlotLayout LAYOUT =
            new PlotLayout(0.0, 10.0, 0.0, 2.0, 110, 60, 10);

    @Nested
    @DisplayName("Plot area dimensions")
    class Area {

        @Test
        @DisplayName("Plot width and height exclude both margins")
        void excludesMargins() {
            assertEquals(90, LAYOUT.plotWidth());
            assertEquals(40, LAYOUT.plotHeight());
        }
    }

    @Nested
    @DisplayName("Horizontal mapping")
    class Horizontal {

        @Test
        @DisplayName("minHeight maps to the left margin")
        void minHeightIsLeftMargin() {
            assertEquals(10.0, LAYOUT.xPixel(0.0), 1e-9);
        }

        @Test
        @DisplayName("maxHeight maps to the right edge, inside the margin")
        void maxHeightIsRightEdge() {
            assertEquals(100.0, LAYOUT.xPixel(10.0), 1e-9);
        }

        @Test
        @DisplayName("The midpoint height maps to the horizontal centre of the plot area")
        void midpointIsCentred() {
            assertEquals(55.0, LAYOUT.xPixel(5.0), 1e-9);
        }
    }

    @Nested
    @DisplayName("Vertical mapping is flipped, like PlaneRegion")
    class Vertical {

        @Test
        @DisplayName("minMagnitude maps to the bottom of the plot area")
        void minMagnitudeIsAtTheBottom() {
            assertEquals(50.0, LAYOUT.yPixel(0.0), 1e-9);
        }

        @Test
        @DisplayName("maxMagnitude maps to the top of the plot area")
        void maxMagnitudeIsAtTheTop() {
            assertEquals(10.0, LAYOUT.yPixel(2.0), 1e-9);
        }

        @Test
        @DisplayName("The midpoint magnitude maps to the vertical centre of the plot area")
        void midpointIsCentred() {
            assertEquals(30.0, LAYOUT.yPixel(1.0), 1e-9);
        }

        @Test
        @DisplayName("The baseline row is where minMagnitude is drawn")
        void baselineMatchesMinMagnitude() {
            assertEquals(LAYOUT.yPixel(0.0), LAYOUT.baselineRow(), 0.0);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("A height range that does not go forward is rejected")
        void rejectsBadHeightRange() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PlotLayout(5.0, 5.0, 0.0, 1.0, 110, 60, 10));
            assertThrows(IllegalArgumentException.class,
                    () -> new PlotLayout(5.0, 1.0, 0.0, 1.0, 110, 60, 10));
        }

        @Test
        @DisplayName("A magnitude range that does not go forward is rejected")
        void rejectsBadMagnitudeRange() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PlotLayout(0.0, 10.0, 1.0, 1.0, 110, 60, 10));
            assertThrows(IllegalArgumentException.class,
                    () -> new PlotLayout(0.0, 10.0, 1.0, 0.0, 110, 60, 10));
        }

        @Test
        @DisplayName("A negative margin is rejected")
        void rejectsNegativeMargin() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PlotLayout(0.0, 10.0, 0.0, 1.0, 110, 60, -1));
        }

        @Test
        @DisplayName("A canvas too small for its own margins is rejected")
        void rejectsUndersizedCanvas() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PlotLayout(0.0, 10.0, 0.0, 1.0, 20, 60, 15));
            assertThrows(IllegalArgumentException.class,
                    () -> new PlotLayout(0.0, 10.0, 0.0, 1.0, 110, 20, 15));
        }
    }
}