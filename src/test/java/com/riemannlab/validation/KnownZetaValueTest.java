package com.riemannlab.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KnownZetaValueTest {

    @Nested
    class Validation {

        @Test
        void rejectsNonFiniteS() {
            assertThrows(IllegalArgumentException.class,
                    () -> new KnownZetaValue(Double.NaN, -1.0 / 12.0));
        }

        @Test
        void rejectsInfiniteS() {
            assertThrows(IllegalArgumentException.class,
                    () -> new KnownZetaValue(Double.POSITIVE_INFINITY, -1.0 / 12.0));
        }

        @Test
        void rejectsNonFiniteExactValue() {
            assertThrows(IllegalArgumentException.class,
                    () -> new KnownZetaValue(-1.0, Double.NaN));
        }
    }

    @Nested
    class Accessors {

        @Test
        void exposesItsComponents() {
            KnownZetaValue value = new KnownZetaValue(-1.0, -1.0 / 12.0);

            assertEquals(-1.0, value.s());
            assertEquals(-1.0 / 12.0, value.exactValue());
        }
    }
}
