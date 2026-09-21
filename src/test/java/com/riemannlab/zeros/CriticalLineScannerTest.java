package com.riemannlab.zeros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.NaiveEtaEvaluator;
import com.riemannlab.zeta.ZetaEvaluator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies that scanning the critical line finds the known zeros, and only
 * those, without being told where they are.
 *
 * <p>The scanner is given no reference values at any point. The published
 * heights appear only in the assertions, as validation of what the scan
 * independently produced. Recovering them is numerical evidence and not a
 * proof of anything about the Riemann Hypothesis.</p>
 *
 * <p>Two levels of result are tested separately. {@code scan} reports every
 * strict local minimum, which is the raw measurement. {@code scanForZeros}
 * keeps only those consistent with a zero on the axis. The difference matters:
 * {@code |zeta(0.5 + it)|} has a genuine local minimum near t = 2.4 with
 * magnitude about 0.53, which is a feature of the curve and not a zero.</p>
 */
class CriticalLineScannerTest {

    private static final double FIRST_ZERO = 14.134725141734693;
    private static final double SECOND_ZERO = 21.022039638771555;
    private static final double THIRD_ZERO = 25.010857580145688;

    private final CriticalLineScanner scanner =
            new CriticalLineScanner(new AcceleratedEtaEvaluator());

    @Nested
    @DisplayName("Finding the known zeros")
    class FindingZeros {

        @Test
        @DisplayName("Scanning t from 10 to 30 finds exactly three candidates")
        void findsExactlyThreeCandidates() {
            List<ZeroCandidate> candidates = scanner.scanForZeros(10.0, 30.0, 0.25);

            assertEquals(3, candidates.size(),
                    "expected the three known zeros in this range, got " + candidates);
        }

        @Test
        @DisplayName("The three candidates bracket the three published zeros, in order")
        void candidatesBracketThePublishedZeros() {
            List<ZeroCandidate> candidates = scanner.scanForZeros(10.0, 30.0, 0.25);

            assertTrue(candidates.get(0).brackets(FIRST_ZERO),
                    candidates.get(0) + " should bracket " + FIRST_ZERO);
            assertTrue(candidates.get(1).brackets(SECOND_ZERO),
                    candidates.get(1) + " should bracket " + SECOND_ZERO);
            assertTrue(candidates.get(2).brackets(THIRD_ZERO),
                    candidates.get(2) + " should bracket " + THIRD_ZERO);
        }

        @Test
        @DisplayName("Estimated heights land within 0.01 of the published values")
        void estimatedHeightsAreClose() {
            List<ZeroCandidate> candidates = scanner.scanForZeros(10.0, 30.0, 0.25);

            assertEquals(FIRST_ZERO, candidates.get(0).estimatedHeight(), 0.01);
            assertEquals(SECOND_ZERO, candidates.get(1).estimatedHeight(), 0.01);
            assertEquals(THIRD_ZERO, candidates.get(2).estimatedHeight(), 0.01);
        }

        @Test
        @DisplayName("A finer step gives a better estimate")
        void finerStepImprovesTheEstimate() {
            double coarseError = Math.abs(FIRST_ZERO
                    - scanner.scanForZeros(13.0, 15.5, 0.25).get(0).estimatedHeight());
            double fineError = Math.abs(FIRST_ZERO
                    - scanner.scanForZeros(13.0, 15.5, 0.05).get(0).estimatedHeight());

            assertTrue(fineError < coarseError,
                    "step 0.25 gave " + coarseError + ", step 0.05 gave " + fineError);
        }

        @Test
        @DisplayName("Every zero candidate drops by at least half its larger neighbour")
        void everyCandidateHasASteepDrop() {
            for (ZeroCandidate candidate : scanner.scanForZeros(10.0, 30.0, 0.25)) {
                assertTrue(candidate.relativeDrop() >= 0.4,
                        candidate + " dropped only " + candidate.relativeDrop());
            }
        }

        @Test
        @DisplayName("Candidates come back in increasing order of height")
        void candidatesAreOrdered() {
            List<ZeroCandidate> candidates = scanner.scanForZeros(10.0, 55.0, 0.2);

            for (int i = 1; i < candidates.size(); i++) {
                assertTrue(candidates.get(i).height() > candidates.get(i - 1).height(),
                        "candidate " + i + " is out of order");
            }
        }

        @Test
        @DisplayName("Scanning to height 55 finds eleven zeros")
        void findsElevenZerosBelowFiftyFive() {
            List<ZeroCandidate> candidates = scanner.scanForZeros(1.0, 55.0, 0.1);

            assertEquals(11, candidates.size(),
                    "there are eleven non-trivial zeros with height below 55, got " + candidates);
        }
    }

    @Nested
    @DisplayName("Distinguishing zeros from mere dips")
    class Discrimination {

        @Test
        @DisplayName("A raw scan below the first zero still finds a local minimum")
        void rawScanFindsTheShallowDipNearTwoPointFour() {
            List<ZeroCandidate> minima = scanner.scan(1.0, 13.0, 0.25);

            assertEquals(1, minima.size(),
                    "|zeta| genuinely dips near t = 2.4; got " + minima);
            assertTrue(minima.get(0).magnitude() > 0.4,
                    "and that dip sits well above the axis, at "
                            + minima.get(0).magnitude());
        }

        @Test
        @DisplayName("Filtering rejects it, because the first zero is at 14.13")
        void filteredScanRejectsTheShallowDip() {
            List<ZeroCandidate> candidates = scanner.scanForZeros(1.0, 13.0, 0.25);

            assertTrue(candidates.isEmpty(),
                    "the first zero is at 14.13, got " + candidates);
        }

        @Test
        @DisplayName("The rejected dip drops by only a few percent")
        void theRejectedDipHasAShallowDrop() {
            ZeroCandidate dip = scanner.scan(1.0, 13.0, 0.25).get(0);

            assertTrue(dip.relativeDrop() < 0.1,
                    "expected a shallow drop, got " + dip.relativeDrop());
        }

        @Test
        @DisplayName("A range between two zeros yields nothing either way")
        void emptyRangeBetweenZerosYieldsNothing() {
            assertTrue(scanner.scan(16.0, 19.0, 0.25).isEmpty());
            assertTrue(scanner.scanForZeros(16.0, 19.0, 0.25).isEmpty());
        }

        @Test
        @DisplayName("Filtering never adds candidates the raw scan did not find")
        void filteringOnlyRemoves() {
            List<ZeroCandidate> raw = scanner.scan(1.0, 30.0, 0.25);
            List<ZeroCandidate> filtered = scanner.scanForZeros(1.0, 30.0, 0.25);

            assertTrue(filtered.size() <= raw.size(),
                    raw.size() + " raw minima but " + filtered.size() + " after filtering");
            assertTrue(raw.containsAll(filtered),
                    "every filtered candidate must have come from the raw scan");
        }
    }

    @Nested
    @DisplayName("Independence from the evaluator")
    class EvaluatorIndependence {

        @Test
        @DisplayName("The naive evaluator finds the same three zeros")
        void naiveEvaluatorAgrees() {
            ZetaEvaluator naive = new NaiveEtaEvaluator(20_000);
            List<ZeroCandidate> candidates =
                    new CriticalLineScanner(naive).scanForZeros(10.0, 30.0, 0.25);

            assertEquals(3, candidates.size());
            assertTrue(candidates.get(0).brackets(FIRST_ZERO));
            assertTrue(candidates.get(1).brackets(SECOND_ZERO));
            assertTrue(candidates.get(2).brackets(THIRD_ZERO));
        }

        @Test
        @DisplayName("Both evaluators estimate the first zero to within 0.01 of each other")
        void bothEvaluatorsAgreeOnTheEstimate() {
            double viaAccelerated =
                    scanner.scanForZeros(13.0, 15.5, 0.25).get(0).estimatedHeight();
            double viaNaive = new CriticalLineScanner(new NaiveEtaEvaluator(20_000))
                    .scanForZeros(13.0, 15.5, 0.25).get(0).estimatedHeight();

            assertEquals(viaAccelerated, viaNaive, 0.01);
        }
    }

    @Nested
    @DisplayName("Zero density and step size")
    class StepSizeGuidance {

        @Test
        @DisplayName("Average spacing shrinks as height grows")
        void spacingShrinksWithHeight() {
            double at30 = CriticalLineScanner.averageZeroSpacing(30);
            double at100 = CriticalLineScanner.averageZeroSpacing(100);
            double at1000 = CriticalLineScanner.averageZeroSpacing(1_000);

            assertTrue(at100 < at30, at30 + " -> " + at100);
            assertTrue(at1000 < at100, at100 + " -> " + at1000);
        }

        @Test
        @DisplayName("Spacing near t = 30 is about 4, matching the observed gaps")
        void spacingMatchesObservedGaps() {
            assertEquals(4.0, CriticalLineScanner.averageZeroSpacing(30), 0.6);
        }

        @Test
        @DisplayName("Spacing stays finite and positive at small heights")
        void spacingIsDefinedAtSmallHeights() {
            for (double height : new double[] {0.5, 1, 5, 6.28, 7}) {
                double spacing = CriticalLineScanner.averageZeroSpacing(height);
                assertTrue(Double.isFinite(spacing) && spacing > 0,
                        "spacing at height " + height + " was " + spacing);
            }
        }

        @Test
        @DisplayName("The recommended step is a fraction of the average spacing")
        void recommendedStepIsFinerThanTheSpacing() {
            double height = 100;
            assertTrue(CriticalLineScanner.recommendedStep(height)
                            < CriticalLineScanner.averageZeroSpacing(height) / 2.0,
                    "the recommended step must comfortably resolve neighbouring zeros");
        }

        @Test
        @DisplayName("The recommended step actually finds the known zeros")
        void recommendedStepWorks() {
            List<ZeroCandidate> candidates =
                    scanner.scanForZeros(10.0, 30.0, CriticalLineScanner.recommendedStep(30.0));

            assertEquals(3, candidates.size(), "got " + candidates);
        }
    }

    @Nested
    @DisplayName("Input validation")
    class Validation {

        @Test
        @DisplayName("A null evaluator is rejected at construction")
        void rejectsNullEvaluator() {
            assertThrows(NullPointerException.class, () -> new CriticalLineScanner(null));
        }

        @Test
        @DisplayName("An inverted or empty range is rejected")
        void rejectsBadRange() {
            assertThrows(IllegalArgumentException.class, () -> scanner.scan(30.0, 10.0, 0.25));
            assertThrows(IllegalArgumentException.class, () -> scanner.scan(10.0, 10.0, 0.25));
        }

        @Test
        @DisplayName("A non-positive step is rejected")
        void rejectsBadStep() {
            assertThrows(IllegalArgumentException.class, () -> scanner.scan(10.0, 30.0, 0.0));
            assertThrows(IllegalArgumentException.class, () -> scanner.scan(10.0, 30.0, -0.25));
        }

        @Test
        @DisplayName("A step larger than the range is rejected")
        void rejectsStepLargerThanRange() {
            assertThrows(IllegalArgumentException.class, () -> scanner.scan(10.0, 11.0, 5.0));
        }

        @Test
        @DisplayName("A start height outside the eta domain is rejected")
        void rejectsNonPositiveStartHeight() {
            assertThrows(IllegalArgumentException.class, () -> scanner.scan(-5.0, 10.0, 0.25));
        }

        @Test
        @DisplayName("Validation applies to the filtered scan too")
        void filteredScanValidatesItsInput() {
            assertThrows(IllegalArgumentException.class,
                    () -> scanner.scanForZeros(30.0, 10.0, 0.25));
        }

        @Test
        @DisplayName("Both returned lists are unmodifiable")
        void resultsAreUnmodifiable() {
            assertThrows(UnsupportedOperationException.class,
                    () -> scanner.scan(10.0, 30.0, 0.25).clear());
            assertThrows(UnsupportedOperationException.class,
                    () -> scanner.scanForZeros(10.0, 30.0, 0.25).clear());
        }
    }
}
