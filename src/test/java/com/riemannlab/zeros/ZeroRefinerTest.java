package com.riemannlab.zeros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.numeric.NewtonRootFinder;
import com.riemannlab.core.numeric.SecantRootFinder;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Validates the complete pipeline: scan the critical line, refine each
 * candidate, and compare against published reference values.
 *
 * <p>Nothing in the search uses the reference values. They appear only in the
 * assertions. Recovering them is numerical evidence that the evaluator, the
 * detector and the root finder all work together; it is not a proof of
 * anything about the Riemann Hypothesis, and it says nothing about zeros
 * outside the scanned range.</p>
 *
 * <p>The tests that begin off the critical line are the interesting ones. The
 * root finder is free to move anywhere in the complex plane, so where it ends
 * up is a measurement rather than an assumption.</p>
 */
class ZeroRefinerTest {

    /** Published reference values for the first eleven non-trivial zero heights. */
    private static final double[] REFERENCE_ZEROS = {
        14.134725141734693,
        21.022039638771555,
        25.010857580145688,
        30.424876125859513,
        32.935061587739189,
        37.586178158825671,
        40.918719012147495,
        43.327073280914999,
        48.005150881167159,
        49.773832477672302,
        52.970321477714460
    };

    private final AcceleratedEtaEvaluator evaluator = new AcceleratedEtaEvaluator();
    private final CriticalLineScanner scanner = new CriticalLineScanner(evaluator);
    private final ZeroRefiner refiner = new ZeroRefiner(evaluator);

    private List<ZeroCandidate> candidates() {
        return scanner.scanForZeros(1.0, 55.0, 0.1);
    }

    @Nested
    @DisplayName("Recovering the published zeros")
    class Recovery {

        @Test
        @DisplayName("All eleven candidates refine to confirmed zeros")
        void allCandidatesConfirm() {
            List<RefinedZero> zeros = refiner.refineAll(candidates());

            assertEquals(11, zeros.size());
            for (RefinedZero zero : zeros) {
                assertTrue(zero.isConfirmed(),
                        "failed at height " + zero.height() + ": " + zero.outcome()
                                + ", residual " + zero.residualMagnitude());
            }
        }

        @Test
        @DisplayName("Every height matches its published value to 1e-11")
        void heightsMatchPublishedValues() {
            List<RefinedZero> zeros = refiner.refineAll(candidates());

            for (int i = 0; i < REFERENCE_ZEROS.length; i++) {
                assertEquals(REFERENCE_ZEROS[i], zeros.get(i).height(), 1e-11,
                        "zero number " + (i + 1));
            }
        }

        @Test
        @DisplayName("The first zero is recovered to about fifteen digits")
        void firstZeroToFifteenDigits() {
            RefinedZero first = refiner.refine(candidates().get(0));

            assertEquals(14.134725141734693, first.height(), 1e-12);
            assertTrue(first.residualMagnitude() < 1e-13,
                    "residual was " + first.residualMagnitude());
        }

        @Test
        @DisplayName("Refinement improves on the scan estimate by many orders of magnitude")
        void refinementBeatsTheScanEstimate() {
            ZeroCandidate candidate = candidates().get(0);
            RefinedZero refined = refiner.refine(candidate);

            double scanError =
                    Math.abs(candidate.estimatedHeight() - REFERENCE_ZEROS[0]);
            double refinedError = Math.abs(refined.height() - REFERENCE_ZEROS[0]);

            assertTrue(refinedError < scanError / 1e6,
                    "scan " + scanError + " -> refined " + refinedError);
        }
    }

    @Nested
    @DisplayName("Where the real part ends up")
    class RealPart {

        @Test
        @DisplayName("Every refined zero has a real part indistinguishable from one half")
        void realPartsLandOnOneHalf() {
            for (RefinedZero zero : refiner.refineAll(candidates())) {
                assertEquals(0.5, zero.realPart(), 1e-11,
                        "at height " + zero.height());
            }
        }

        @Test
        @DisplayName("A search started well to the right of the line converges onto it")
        void convergesFromTheRight() {
            RefinedZero zero = refiner.refineFrom(Complex.of(0.7, 14.1));

            assertTrue(zero.isConfirmed(), zero.outcome().toString());
            assertEquals(0.5, zero.realPart(), 1e-9,
                    "started at Re(s) = 0.7 and should have found Re(s) = 0.5");
            assertEquals(REFERENCE_ZEROS[0], zero.height(), 1e-9);
        }

        @Test
        @DisplayName("A search started to the left of the line also converges onto it")
        void convergesFromTheLeft() {
            RefinedZero zero = refiner.refineFrom(Complex.of(0.35, 21.0));

            assertTrue(zero.isConfirmed(), zero.outcome().toString());
            assertEquals(0.5, zero.realPart(), 1e-9,
                    "started at Re(s) = 0.35 and should have found Re(s) = 0.5");
            assertEquals(REFERENCE_ZEROS[1], zero.height(), 1e-9);
        }

        @Test
        @DisplayName("The deviation from the line is reported, not silently discarded")
        void deviationIsReported() {
            RefinedZero zero = refiner.refineFrom(Complex.of(0.7, 14.1));

            assertTrue(zero.deviationFromCriticalLine() >= 0.0);
            assertTrue(zero.deviationFromCriticalLine() < 1e-9,
                    "deviation was " + zero.deviationFromCriticalLine());
        }
    }

    @Nested
    @DisplayName("Cost")
    class Cost {

        @Test
        @DisplayName("Each zero costs only a handful of zeta evaluations")
        void refinementIsCheap() {
            for (RefinedZero zero : refiner.refineAll(candidates())) {
                assertTrue(zero.zetaEvaluations() <= 30,
                        "height " + zero.height() + " took "
                                + zero.zetaEvaluations() + " evaluations");
            }
        }

        @Test
        @DisplayName("Each zero converges in a handful of iterations")
        void convergesQuickly() {
            for (RefinedZero zero : refiner.refineAll(candidates())) {
                assertTrue(zero.iterations() <= 15,
                        "height " + zero.height() + " took "
                                + zero.iterations() + " iterations");
            }
        }

        @Test
        @DisplayName("Refining all eleven costs far less than the scan that found them")
        void refinementIsCheaperThanScanning() {
            int refinementCost = refiner.refineAll(candidates()).stream()
                    .mapToInt(RefinedZero::zetaEvaluations)
                    .sum();

            int scanCost = 541;

            assertTrue(refinementCost < scanCost,
                    "refinement " + refinementCost + " vs scan " + scanCost
                            + "; finding the neighbourhoods is the expensive part");
        }
    }

    @Nested
    @DisplayName("Choice of root finder")
    class RootFinderChoice {

        @Test
        @DisplayName("Newton finds the same zero as the secant method")
        void newtonAgreesWithSecant() {
            ZeroCandidate candidate = candidates().get(0);

            RefinedZero viaSecant =
                    new ZeroRefiner(evaluator, new SecantRootFinder()).refine(candidate);
            RefinedZero viaNewton =
                    new ZeroRefiner(evaluator, new NewtonRootFinder()).refine(candidate);

            assertTrue(viaSecant.isConfirmed());
            assertTrue(viaNewton.isConfirmed());
            assertEquals(viaSecant.height(), viaNewton.height(), 1e-11);
        }

        @Test
        @DisplayName("The secant method costs fewer zeta evaluations than Newton")
        void secantIsCheaperOnZeta() {
            ZeroCandidate candidate = candidates().get(0);

            int secantCost = new ZeroRefiner(evaluator, new SecantRootFinder())
                    .refine(candidate).zetaEvaluations();
            int newtonCost = new ZeroRefiner(evaluator, new NewtonRootFinder())
                    .refine(candidate).zetaEvaluations();

            assertTrue(secantCost < newtonCost,
                    "secant " + secantCost + " vs Newton " + newtonCost);
        }
    }

    @Nested
    @DisplayName("Failure handling")
    class Failures {

        @Test
        @DisplayName("A hopeless starting point fails without throwing")
        void hopelessStartDoesNotThrow() {
            RefinedZero zero = refiner.refineFrom(Complex.of(0.5, 17.75));

            assertFalse(zero.isConfirmed(),
                    "there is no zero at t = 17.75, outcome was " + zero.outcome());
            assertTrue(Double.isFinite(zero.location().real())
                            && Double.isFinite(zero.location().imaginary()),
                    "the reported point must remain finite, was " + zero.location());
        }

        @Test
        @DisplayName("Leaving the evaluator's domain is reported rather than propagated")
        void leavingTheDomainIsReported() {
            RefinedZero zero = refiner.refineFrom(Complex.of(0.01, 6.0));

            assertFalse(zero.isConfirmed());
            assertTrue(Double.isFinite(zero.location().real()),
                    "should have failed cleanly, got " + zero.location());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Null constructor arguments are rejected")
        void rejectsNullDependencies() {
            assertThrows(NullPointerException.class, () -> new ZeroRefiner(null));
            assertThrows(NullPointerException.class,
                    () -> new ZeroRefiner(evaluator, null));
        }

        @Test
        @DisplayName("Null arguments to refine are rejected")
        void rejectsNullArguments() {
            assertThrows(NullPointerException.class, () -> refiner.refine(null));
            assertThrows(NullPointerException.class, () -> refiner.refineFrom(null));
            assertThrows(NullPointerException.class, () -> refiner.refineAll(null));
        }

        @Test
        @DisplayName("Refining an empty list gives an empty result")
        void emptyInputGivesEmptyOutput() {
            assertTrue(refiner.refineAll(List.of()).isEmpty());
        }

        @Test
        @DisplayName("The returned list is unmodifiable and in the same order")
        void resultIsUnmodifiableAndOrdered() {
            List<RefinedZero> zeros = refiner.refineAll(candidates());

            assertThrows(UnsupportedOperationException.class, zeros::clear);
            for (int i = 1; i < zeros.size(); i++) {
                assertTrue(zeros.get(i).height() > zeros.get(i - 1).height(),
                        "order was not preserved at index " + i);
            }
        }
    }
}
