package com.riemannlab.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeros.RefinedZero;
import com.riemannlab.zeros.RefinedZero.Outcome;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ResidualReportTest {

    private static RefinedZero zero(
            double height, double residual, double errorBound, Outcome outcome) {
        Complex location = Complex.of(0.5, height);
        return new RefinedZero(location, location, residual, errorBound, 5, 8, outcome);
    }

    @Nested
    @DisplayName("Counting outcomes")
    class Counting {

        @Test
        @DisplayName("Total counts every entry, regardless of outcome")
        void totalCountsEverything() {
            ResidualReport report = new ResidualReport(List.of(
                    zero(14.13, 1e-15, 1e-13, Outcome.CONFIRMED),
                    zero(21.02, 1.0, 1e-13, Outcome.RESIDUAL_ABOVE_BOUND),
                    zero(30.0, 0.5, 1e-13, Outcome.NOT_CONVERGED)));

            assertEquals(3, report.total());
        }

        @Test
        @DisplayName("countOf reports how many entries had a given outcome")
        void countOfMatchesTheOutcome() {
            ResidualReport report = new ResidualReport(List.of(
                    zero(14.13, 1e-15, 1e-13, Outcome.CONFIRMED),
                    zero(21.02, 1e-14, 1e-13, Outcome.CONFIRMED),
                    zero(30.0, 0.5, 1e-13, Outcome.NOT_CONVERGED)));

            assertEquals(2, report.countOf(Outcome.CONFIRMED));
            assertEquals(1, report.countOf(Outcome.NOT_CONVERGED));
            assertEquals(0, report.countOf(Outcome.LEFT_DOMAIN));
        }

        @Test
        @DisplayName("outcomeCounts covers every possible outcome, even ones with zero entries")
        void outcomeCountsCoversEveryOutcome() {
            ResidualReport report = new ResidualReport(List.of(
                    zero(14.13, 1e-15, 1e-13, Outcome.CONFIRMED)));

            assertEquals(4, report.outcomeCounts().size());
            assertEquals(1L, report.outcomeCounts().get(Outcome.CONFIRMED));
            assertEquals(0L, report.outcomeCounts().get(Outcome.NOT_CONVERGED));
            assertEquals(0L, report.outcomeCounts().get(Outcome.RESIDUAL_ABOVE_BOUND));
            assertEquals(0L, report.outcomeCounts().get(Outcome.LEFT_DOMAIN));
        }
    }

    @Nested
    @DisplayName("Confirmed-zero statistics")
    class ConfirmedStatistics {

        @Test
        @DisplayName("confirmed() keeps only the confirmed entries")
        void confirmedFiltersToConfirmedOnly() {
            RefinedZero confirmedZero = zero(14.13, 1e-15, 1e-13, Outcome.CONFIRMED);
            ResidualReport report = new ResidualReport(List.of(
                    confirmedZero,
                    zero(30.0, 0.5, 1e-13, Outcome.NOT_CONVERGED)));

            assertEquals(List.of(confirmedZero), report.confirmed());
        }

        @Test
        @DisplayName("worstConfirmedResidual is the largest residual among confirmed zeros")
        void worstConfirmedResidualIsTheMaximum() {
            ResidualReport report = new ResidualReport(List.of(
                    zero(14.13, 1e-15, 1e-13, Outcome.CONFIRMED),
                    zero(21.02, 3e-14, 1e-13, Outcome.CONFIRMED),
                    zero(30.0, 0.5, 1e-13, Outcome.NOT_CONVERGED)));

            assertEquals(3e-14, report.worstConfirmedResidual(), 0.0);
        }

        @Test
        @DisplayName("worstConfirmedResidual is NaN when nothing was confirmed")
        void worstConfirmedResidualIsNaNWhenNothingConfirmed() {
            ResidualReport report = new ResidualReport(List.of(
                    zero(30.0, 0.5, 1e-13, Outcome.NOT_CONVERGED)));

            assertTrue(Double.isNaN(report.worstConfirmedResidual()));
        }

        @Test
        @DisplayName("worstConfirmedResidualRatio is the largest residual/bound ratio")
        void worstConfirmedResidualRatioIsTheMaximum() {
            ResidualReport report = new ResidualReport(List.of(
                    zero(14.13, 2e-14, 1e-13, Outcome.CONFIRMED),  // ratio 0.2
                    zero(21.02, 9e-14, 1e-13, Outcome.CONFIRMED))); // ratio 0.9

            assertEquals(0.9, report.worstConfirmedResidualRatio(), 1e-9);
        }

        @Test
        @DisplayName("worstDeviationFromCriticalLine looks only at confirmed zeros")
        void worstDeviationLooksOnlyAtConfirmed() {
            Complex offLine = Complex.of(0.6, 99.0);
            RefinedZero notConfirmedButOffLine =
                    new RefinedZero(offLine, offLine, 5.0, 1e-13, 50, 100, Outcome.NOT_CONVERGED);

            ResidualReport report = new ResidualReport(List.of(
                    zero(14.13, 1e-15, 1e-13, Outcome.CONFIRMED),
                    notConfirmedButOffLine));

            assertEquals(0.0, report.worstDeviationFromCriticalLine(), 1e-9,
                    "the only CONFIRMED entry sits exactly on Re(s) = 0.5");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("A null list is rejected")
        void rejectsNullList() {
            assertThrows(NullPointerException.class, () -> new ResidualReport(null));
        }

        @Test
        @DisplayName("An empty list is rejected")
        void rejectsEmptyList() {
            assertThrows(IllegalArgumentException.class, () -> new ResidualReport(List.of()));
        }

        @Test
        @DisplayName("The entries list is immutable")
        void entriesListIsImmutable() {
            List<RefinedZero> mutable = new ArrayList<>();
            mutable.add(zero(14.13, 1e-15, 1e-13, Outcome.CONFIRMED));
            ResidualReport report = new ResidualReport(mutable);

            mutable.add(zero(21.02, 1e-14, 1e-13, Outcome.CONFIRMED));

            assertEquals(1, report.total(),
                    "mutating the list passed to the constructor must not affect the report");
        }
    }
}