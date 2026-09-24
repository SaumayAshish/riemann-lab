package com.riemannlab.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PrecisionCheckerTest {

    private static final AcceleratedEtaEvaluator SERIES = new AcceleratedEtaEvaluator();
    private static final ContinuedZetaEvaluator CONTINUED = new ContinuedZetaEvaluator(SERIES);

    @Nested
    class Validation {

        @Test
        void rejectsNullEvaluator() {
            assertThrows(NullPointerException.class,
                    () -> PrecisionChecker.check(null, PrecisionChecker.NEGATIVE_ODD_INTEGER_REFERENCES));
        }

        @Test
        void rejectsNullReferences() {
            assertThrows(NullPointerException.class, () -> PrecisionChecker.check(CONTINUED, null));
        }

        @Test
        void rejectsEmptyReferences() {
            assertThrows(IllegalArgumentException.class,
                    () -> PrecisionChecker.check(CONTINUED, List.of()));
        }

        @Test
        void rejectsReferencesContainingNull() {
            List<KnownZetaValue> references = new ArrayList<>();
            references.add(new KnownZetaValue(-1.0, -1.0 / 12.0));
            references.add(null);

            assertThrows(NullPointerException.class, () -> PrecisionChecker.check(CONTINUED, references));
        }
    }

    @Nested
    class Checking {

        @Test
        void producesOneSamplePerReference() {
            List<PrecisionSample> samples =
                    PrecisionChecker.check(CONTINUED, PrecisionChecker.NEGATIVE_ODD_INTEGER_REFERENCES);

            assertEquals(6, samples.size());
        }

        @Test
        void samplesCarryTheReferencePointAndExactValue() {
            KnownZetaValue reference = new KnownZetaValue(-1.0, -1.0 / 12.0);

            List<PrecisionSample> samples = PrecisionChecker.check(CONTINUED, List.of(reference));

            assertEquals(-1.0, samples.get(0).s());
            assertEquals(-1.0 / 12.0, samples.get(0).exactValue());
        }

        @Test
        void continuedEvaluatorStaysWellWithinItsOwnClaimedBoundAtKnownValues() {
            List<PrecisionSample> samples =
                    PrecisionChecker.check(CONTINUED, PrecisionChecker.NEGATIVE_ODD_INTEGER_REFERENCES);

            for (PrecisionSample sample : samples) {
                assertTrue(sample.isWithinClaimedBound(),
                        "expected s=" + sample.s() + " to stay within its claimed error bound, but actual error "
                                + sample.actualError() + " exceeded bound " + sample.claimedErrorBound());
            }
        }
    }
}