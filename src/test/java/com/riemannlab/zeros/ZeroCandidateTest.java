package com.riemannlab.zeros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the linear model a candidate uses to estimate where the zero
 * actually lies inside its bracket.
 *
 * <p>The numbers in these tests are real measurements taken from a scan of the
 * critical line at step 0.25, not invented figures. Because the non-trivial
 * zeros are simple, {@code |zeta|} grows linearly away from them, so three
 * samples determine a V shape whose vertex is the zero.</p>
 *
 * <p>The published zero heights are reference values used for validation.
 * Agreement with them is numerical evidence, never proof of anything.</p>
 */
class ZeroCandidateTest {

    private static final double FIRST_ZERO = 14.134725141734693;
    private static final double SECOND_ZERO = 21.022039638771555;
    private static final double THIRD_ZERO = 25.010857580145688;

    /** Measured at t = 14.25 with step 0.25: neighbours at 14.00 and 14.50. */
    private static ZeroCandidate firstZeroCandidate() {
        return new ZeroCandidate(14.25, 0.092862, 0.25, 0.104968, 0.295882, 2.98e-03);
    }

    /** Measured at t = 21.00 with step 0.25: neighbours at 20.75 and 21.25. */
    private static ZeroCandidate secondZeroCandidate() {
        return new ZeroCandidate(21.00, 0.023544, 0.25, 0.313855, 0.254844, 2.98e-03);
    }

    /** Measured at t = 25.00 with step 0.25: neighbours at 24.75 and 25.25. */
    private static ZeroCandidate thirdZeroCandidate() {
        return new ZeroCandidate(25.00, 0.012909, 0.25, 0.344569, 0.334962, 2.98e-03);
    }

    @Nested
    @DisplayName("The bracket")
    class Bracket {

        @Test
        @DisplayName("The bracket spans one step either side of the sampled minimum")
        void bracketSpansOneStepEitherSide() {
            ZeroCandidate candidate = firstZeroCandidate();

            assertEquals(14.00, candidate.bracketLower(), 1e-12);
            assertEquals(14.50, candidate.bracketUpper(), 1e-12);
            assertEquals(0.50, candidate.bracketWidth(), 1e-12);
        }

        @Test
        @DisplayName("Each published zero lies inside its own bracket")
        void publishedZerosLieInsideTheirBrackets() {
            assertTrue(firstZeroCandidate().brackets(FIRST_ZERO));
            assertTrue(secondZeroCandidate().brackets(SECOND_ZERO));
            assertTrue(thirdZeroCandidate().brackets(THIRD_ZERO));
        }

        @Test
        @DisplayName("A height outside the bracket is not bracketed")
        void unrelatedHeightIsNotBracketed() {
            assertFalse(firstZeroCandidate().brackets(SECOND_ZERO));
            assertFalse(firstZeroCandidate().brackets(17.5));
        }
    }

    @Nested
    @DisplayName("The linear model")
    class LinearModel {

        @Test
        @DisplayName("The slope estimate recovers |zeta'| near the first zero")
        void slopeEstimateIsPlausible() {
            double slope = firstZeroCandidate().estimatedSlope();

            assertEquals(0.8017, slope, 1e-3,
                    "the arms of the V give |zeta'| at the zero");
        }

        @Test
        @DisplayName("The distance estimate is the depth of the V, not the bracket width")
        void distanceEstimateIsMagnitudeOverSlope() {
            ZeroCandidate candidate = firstZeroCandidate();

            assertEquals(candidate.magnitude() / candidate.estimatedSlope(),
                    candidate.estimatedDistanceToZero(), 1e-12);
        }

        @Test
        @DisplayName("The direction is toward the smaller neighbour")
        void directionFollowsTheSmallerNeighbour() {
            assertTrue(firstZeroCandidate().estimatedHeight() < 14.25,
                    "the lower neighbour is smaller, so the zero lies below");
            assertTrue(secondZeroCandidate().estimatedHeight() > 21.00,
                    "the upper neighbour is smaller, so the zero lies above");
        }

        @Test
        @DisplayName("Three grid samples locate each zero to within 0.005")
        void estimatesLandCloseToThePublishedZeros() {
            assertEquals(FIRST_ZERO, firstZeroCandidate().estimatedHeight(), 5e-3);
            assertEquals(SECOND_ZERO, secondZeroCandidate().estimatedHeight(), 5e-3);
            assertEquals(THIRD_ZERO, thirdZeroCandidate().estimatedHeight(), 5e-3);
        }

        @Test
        @DisplayName("The estimate always stays inside the bracket it came from")
        void estimateStaysInsideTheBracket() {
            for (ZeroCandidate candidate : new ZeroCandidate[] {
                    firstZeroCandidate(), secondZeroCandidate(), thirdZeroCandidate()}) {
                assertTrue(candidate.brackets(candidate.estimatedHeight()),
                        candidate + " estimated a height outside its own bracket");
            }
        }

        @Test
        @DisplayName("A flat minimum degrades gracefully instead of dividing by zero")
        void flatMinimumDoesNotDivideByZero() {
            ZeroCandidate flat = new ZeroCandidate(20.0, 0.0, 0.25, 0.0, 0.0, 1e-14);

            assertEquals(0.0, flat.estimatedDistanceToZero(), 1e-12);
            assertEquals(20.0, flat.estimatedHeight(), 1e-12);
        }
    }

    @Nested
    @DisplayName("Plausibility")
    class Plausibility {

        @Test
        @DisplayName("A genuine dip is reported as looking like a zero")
        void genuineDipLooksLikeAZero() {
            assertTrue(firstZeroCandidate().looksLikeZero());
            assertTrue(secondZeroCandidate().looksLikeZero());
            assertTrue(thirdZeroCandidate().looksLikeZero());
        }

        @Test
        @DisplayName("A shallow local minimum well above the axis does not")
        void shallowMinimumIsRejected() {
            ZeroCandidate shallow =
                    new ZeroCandidate(18.0, 1.90, 0.25, 1.95, 1.94, 2.98e-03);

            assertFalse(shallow.looksLikeZero(),
                    "a dip from 1.95 to 1.90 implies the axis is far below the bracket");
        }
    }

    @Nested
    @DisplayName("Input validation")
    class Validation {

        @Test
        @DisplayName("A non-positive step is rejected")
        void rejectsNonPositiveStep() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroCandidate(14.25, 0.09, 0.0, 0.10, 0.29, 1e-3));
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroCandidate(14.25, 0.09, -0.25, 0.10, 0.29, 1e-3));
        }

        @Test
        @DisplayName("Negative magnitudes are rejected, since a magnitude cannot be negative")
        void rejectsNegativeMagnitudes() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroCandidate(14.25, -0.09, 0.25, 0.10, 0.29, 1e-3));
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroCandidate(14.25, 0.09, 0.25, -0.10, 0.29, 1e-3));
        }

        @Test
        @DisplayName("A candidate that is not a local minimum is rejected")
        void rejectsNonMinimum() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ZeroCandidate(14.25, 0.30, 0.25, 0.10, 0.29, 1e-3));
        }
    }
}
