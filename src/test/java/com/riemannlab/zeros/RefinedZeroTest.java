package com.riemannlab.zeros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the report produced for a refined zero.
 *
 * <p>One thing this class deliberately does <em>not</em> do is treat distance
 * from the critical line as a defect. The deviation is reported as a
 * measurement. If a zero were ever found off the line that would be a result,
 * not a bug, and building "off the line means failure" into the code would
 * assume the Riemann Hypothesis rather than test it.</p>
 */
class RefinedZeroTest {

    private static final Complex FIRST_ZERO_LOCATION =
            Complex.of(0.5000000000000002, 14.134725141734695);

    private static RefinedZero confirmedFirstZero() {
        return new RefinedZero(
                Complex.of(0.5, 14.134835923235983),
                FIRST_ZERO_LOCATION,
                7.6e-16,
                1.94e-14,
                4,
                6,
                RefinedZero.Outcome.CONFIRMED);
    }

    @Nested
    @DisplayName("Reading the location")
    class Location {

        @Test
        @DisplayName("Height and real part are read off the refined point")
        void heightAndRealPart() {
            RefinedZero zero = confirmedFirstZero();

            assertEquals(14.134725141734695, zero.height(), 1e-15);
            assertEquals(0.5000000000000002, zero.realPart(), 1e-15);
        }

        @Test
        @DisplayName("Deviation from the critical line is |Re(s) - 1/2|")
        void deviationFromCriticalLine() {
            RefinedZero zero = confirmedFirstZero();

            assertEquals(2e-16, zero.deviationFromCriticalLine(), 1e-15);
        }

        @Test
        @DisplayName("Deviation is reported for points well off the line, not rejected")
        void deviationIsReportedNotRejected() {
            RefinedZero hypothetical = new RefinedZero(
                    Complex.of(0.7, 30.0),
                    Complex.of(0.63, 30.0),
                    1e-16,
                    1e-14,
                    5,
                    7,
                    RefinedZero.Outcome.CONFIRMED);

            assertEquals(0.13, hypothetical.deviationFromCriticalLine(), 1e-12);
            assertTrue(hypothetical.isConfirmed(),
                    "a zero is confirmed by its residual, not by its position;"
                            + " judging position here would assume the hypothesis");
        }

        @Test
        @DisplayName("The distance travelled from the starting point is available")
        void distanceMoved() {
            RefinedZero zero = confirmedFirstZero();

            assertEquals(1.108e-4, zero.distanceMoved(), 1e-6,
                    "the scan estimate was about 1.1e-4 away from the true zero");
        }
    }

    @Nested
    @DisplayName("Outcomes")
    class Outcomes {

        @Test
        @DisplayName("Only CONFIRMED counts as a confirmed zero")
        void onlyConfirmedIsConfirmed() {
            for (RefinedZero.Outcome outcome : RefinedZero.Outcome.values()) {
                RefinedZero zero = new RefinedZero(
                        Complex.of(0.5, 14.0), FIRST_ZERO_LOCATION,
                        1e-16, 1e-14, 4, 6, outcome);

                assertEquals(outcome == RefinedZero.Outcome.CONFIRMED, zero.isConfirmed(),
                        "mismatch for " + outcome);
            }
        }

        @Test
        @DisplayName("A non-converged refinement is not confirmed")
        void nonConvergedIsNotConfirmed() {
            RefinedZero zero = new RefinedZero(
                    Complex.of(0.5, 14.0), Complex.of(0.5, 14.02),
                    3.1e-2, 1.9e-14, 100, 102,
                    RefinedZero.Outcome.NOT_CONVERGED);

            assertFalse(zero.isConfirmed());
        }

        @Test
        @DisplayName("A refinement that left the evaluator's domain is not confirmed")
        void leftDomainIsNotConfirmed() {
            RefinedZero zero = new RefinedZero(
                    Complex.of(0.5, 14.0), Complex.of(0.5, 14.0),
                    0.0, 1.9e-14, 0, 1,
                    RefinedZero.Outcome.LEFT_DOMAIN);

            assertFalse(zero.isConfirmed());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Negative residual or bound is rejected")
        void rejectsNegativeMeasurements() {
            assertThrows(IllegalArgumentException.class,
                    () -> new RefinedZero(Complex.of(0.5, 14.0), FIRST_ZERO_LOCATION,
                            -1e-16, 1e-14, 4, 6, RefinedZero.Outcome.CONFIRMED));
            assertThrows(IllegalArgumentException.class,
                    () -> new RefinedZero(Complex.of(0.5, 14.0), FIRST_ZERO_LOCATION,
                            1e-16, -1e-14, 4, 6, RefinedZero.Outcome.CONFIRMED));
        }

        @Test
        @DisplayName("Negative costs are rejected")
        void rejectsNegativeCosts() {
            assertThrows(IllegalArgumentException.class,
                    () -> new RefinedZero(Complex.of(0.5, 14.0), FIRST_ZERO_LOCATION,
                            1e-16, 1e-14, -1, 6, RefinedZero.Outcome.CONFIRMED));
            assertThrows(IllegalArgumentException.class,
                    () -> new RefinedZero(Complex.of(0.5, 14.0), FIRST_ZERO_LOCATION,
                            1e-16, 1e-14, 4, -1, RefinedZero.Outcome.CONFIRMED));
        }

        @Test
        @DisplayName("Null components are rejected")
        void rejectsNulls() {
            assertThrows(NullPointerException.class,
                    () -> new RefinedZero(null, FIRST_ZERO_LOCATION,
                            1e-16, 1e-14, 4, 6, RefinedZero.Outcome.CONFIRMED));
            assertThrows(NullPointerException.class,
                    () -> new RefinedZero(Complex.of(0.5, 14.0), null,
                            1e-16, 1e-14, 4, 6, RefinedZero.Outcome.CONFIRMED));
            assertThrows(NullPointerException.class,
                    () -> new RefinedZero(Complex.of(0.5, 14.0), FIRST_ZERO_LOCATION,
                            1e-16, 1e-14, 4, 6, null));
        }
    }
}
