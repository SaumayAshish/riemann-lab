package com.riemannlab.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ZetaAccuracyCheckerTest {

    private static final AcceleratedEtaEvaluator SERIES = new AcceleratedEtaEvaluator();
    private static final ContinuedZetaEvaluator CONTINUED = new ContinuedZetaEvaluator(SERIES);

    @Nested
    class Validation {

        @Test
        void rejectsNullDirectEvaluator() {
            assertThrows(NullPointerException.class,
                    () -> ZetaAccuracyChecker.check(null, CONTINUED, List.of(Complex.of(0.3, 5.0))));
        }

        @Test
        void rejectsNullReflectedEvaluator() {
            assertThrows(NullPointerException.class,
                    () -> ZetaAccuracyChecker.check(SERIES, null, List.of(Complex.of(0.3, 5.0))));
        }

        @Test
        void rejectsNullPoints() {
            assertThrows(NullPointerException.class,
                    () -> ZetaAccuracyChecker.check(SERIES, CONTINUED, null));
        }

        @Test
        void rejectsEmptyPoints() {
            assertThrows(IllegalArgumentException.class,
                    () -> ZetaAccuracyChecker.check(SERIES, CONTINUED, List.of()));
        }

        @Test
        void rejectsListContainingNull() {
            List<Complex> points = new ArrayList<>();
            points.add(Complex.of(0.3, 5.0));
            points.add(null);

            assertThrows(NullPointerException.class,
                    () -> ZetaAccuracyChecker.check(SERIES, CONTINUED, points));
        }
    }

    @Nested
    class Checking {

        @Test
        void producesOneSamplePerPoint() {
            List<Complex> points = List.of(Complex.of(0.25, 3.0), Complex.of(0.40, 20.0));

            List<ZetaAgreementSample> samples = ZetaAccuracyChecker.check(SERIES, CONTINUED, points);

            assertEquals(2, samples.size());
        }

        @Test
        void samplesCarryTheRequestedPoint() {
            Complex point = Complex.of(0.30, -5.0);

            List<ZetaAgreementSample> samples = ZetaAccuracyChecker.check(SERIES, CONTINUED, List.of(point));

            assertEquals(point, samples.get(0).point());
        }

        @Test
        void directAndReflectedRoutesAgreeToNearMachinePrecision() {
            // Matches ContinuationDemo's own cross-validation: independent
            // computations of zeta agree to roughly 1e-13 relative for these
            // points, deep inside 0 < Re(s) < 1/2.
            List<Complex> points = List.of(
                    Complex.of(0.25, 3.0), Complex.of(0.10, 7.0), Complex.of(0.45, 30.0));

            List<ZetaAgreementSample> samples = ZetaAccuracyChecker.check(SERIES, CONTINUED, points);

            for (ZetaAgreementSample sample : samples) {
                assertTrue(sample.relativeDifference() < 1e-10,
                        "expected agreement at " + sample.point() + " but got relative difference "
                                + sample.relativeDifference());
            }
        }
    }
}