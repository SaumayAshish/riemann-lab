package com.riemannlab.zeta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Dirichlet eta function against known values, and pins down the
 * property that makes it useful: it converges throughout the critical strip,
 * where the defining zeta series does not.
 */
class EtaFunctionTest {

    private static final double TOLERANCE = 1e-12;

    /** eta(1) = ln 2. */
    private static final double ETA_AT_ONE = 0.6931471805599453;

    /** eta(2) = pi^2 / 12. */
    private static final double ETA_AT_TWO = Math.PI * Math.PI / 12.0;

    /** Imaginary part of the first non-trivial zero. A published reference value. */
    private static final double FIRST_ZERO_HEIGHT = 14.134725141734693;

    @Nested
    @DisplayName("The alternating definition")
    class Definition {

        @Test
        @DisplayName("One term is 1, because the first sign is positive")
        void firstTermIsPositiveOne() {
            Complex sum = EtaFunction.partialSum(Complex.ofReal(2), 1);
            assertEquals(1.0, sum.real(), TOLERANCE);
            assertEquals(0.0, sum.imaginary(), TOLERANCE);
        }

        @Test
        @DisplayName("Two terms give 1 - 2^-s, because the second sign is negative")
        void secondTermIsSubtracted() {
            Complex sum = EtaFunction.partialSum(Complex.ofReal(2), 2);
            assertEquals(1.0 - 0.25, sum.real(), TOLERANCE);
        }

        @Test
        @DisplayName("Ten terms of the alternating harmonic series give 0.6456349206349206")
        void tenTermsOfTheAlternatingHarmonicSeries() {
            Complex sum = EtaFunction.partialSum(Complex.ONE, 10);
            assertEquals(0.6456349206349206, sum.real(), 1e-12);
            assertEquals(0.0, sum.imaginary(), TOLERANCE);
        }

        @Test
        @DisplayName("Partial sums alternate around the limit, bracketing it")
        void partialSumsBracketTheLimit() {
            double oddStop = EtaFunction.partialSum(Complex.ONE, 101).real();
            double evenStop = EtaFunction.partialSum(Complex.ONE, 100).real();

            assertTrue(evenStop < ETA_AT_ONE,
                    "stopping on a subtraction should undershoot: " + evenStop);
            assertTrue(oddStop > ETA_AT_ONE,
                    "stopping on an addition should overshoot: " + oddStop);
        }
    }

    @Nested
    @DisplayName("Convergence to known values")
    class KnownValues {

        @Test
        @DisplayName("eta(1) converges to ln 2, where the zeta series diverged")
        void etaAtOneIsLogTwo() {
            Complex sum = EtaFunction.partialSum(Complex.ONE, 100_000);
            assertEquals(ETA_AT_ONE, sum.real(), 1e-4);
        }

        @Test
        @DisplayName("eta(2) converges to pi^2/12")
        void etaAtTwoIsPiSquaredOverTwelve() {
            Complex sum = EtaFunction.partialSum(Complex.ofReal(2), 10_000);
            assertEquals(ETA_AT_TWO, sum.real(), 1e-6);
        }

        @Test
        @DisplayName("The error is bounded by the size of the next term")
        void errorIsBoundedByTheNextTerm() {
            int termCount = 1_000;
            double error = Math.abs(ETA_AT_ONE
                    - EtaFunction.partialSum(Complex.ONE, termCount).real());
            double nextTermSize = 1.0 / (termCount + 1);

            assertTrue(error <= nextTermSize,
                    "Leibniz guarantees error <= next term: error was " + error
                            + ", next term is " + nextTermSize);
        }
    }

    @Nested
    @DisplayName("Behaviour on the critical line")
    class CriticalLine {

        @Test
        @DisplayName("Partial sums stay bounded where the zeta series ran away")
        void partialSumsStayBounded() {
            Complex s = Complex.of(0.5, FIRST_ZERO_HEIGHT);

            double etaAt10000 = EtaFunction.partialSum(s, 10_000).magnitude();
            double etaAt100000 = EtaFunction.partialSum(s, 100_000).magnitude();

            assertTrue(etaAt10000 < 1.0,
                    "eta partial sum should stay small, was " + etaAt10000);
            assertTrue(etaAt100000 < 1.0,
                    "eta partial sum should stay small, was " + etaAt100000);
        }

        @Test
        @DisplayName("Eta is dramatically better behaved than the defining series")
        void etaBeatsTheDefiningSeriesOnTheCriticalLine() {
            Complex s = Complex.of(0.5, FIRST_ZERO_HEIGHT);
            int termCount = 100_000;

            double etaMagnitude = EtaFunction.partialSum(s, termCount).magnitude();
            double zetaSeriesMagnitude = DirichletSeries.partialSum(s, termCount).magnitude();

            assertTrue(zetaSeriesMagnitude > 20.0,
                    "the defining series should be diverging, was " + zetaSeriesMagnitude);
            assertTrue(etaMagnitude < zetaSeriesMagnitude / 20.0,
                    "eta (" + etaMagnitude + ") should be far smaller than the defining series ("
                            + zetaSeriesMagnitude + ")");
        }
    }

    @Nested
    @DisplayName("Input validation")
    class Validation {

        @Test
        @DisplayName("A term count below one is rejected")
        void rejectsNonPositiveTermCount() {
            Complex s = Complex.ofReal(2);
            assertThrows(IllegalArgumentException.class, () -> EtaFunction.partialSum(s, 0));
            assertThrows(IllegalArgumentException.class, () -> EtaFunction.partialSum(s, -5));
        }
    }
}
