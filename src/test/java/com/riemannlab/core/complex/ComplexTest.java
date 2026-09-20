package com.riemannlab.core.complex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the complex arithmetic that every later component of RiemannLab
 * depends on.
 *
 * <p>Comparisons are made component-wise within a tolerance rather than with
 * {@code equals}, because floating-point results are approximations. This is
 * the testing discipline the whole project follows.</p>
 */
class ComplexTest {

    /**
     * Comparison tolerance. Machine epsilon is about 2.2e-16; 1e-12 leaves
     * generous headroom for accumulated rounding across a few operations
     * while still being far tighter than any real error would be.
     */
    private static final double TOLERANCE = 1e-12;

    private static void assertComplexEquals(Complex expected, Complex actual) {
        assertEquals(expected.real(), actual.real(), TOLERANCE,
                () -> "Real part mismatch: expected " + expected + " but was " + actual);
        assertEquals(expected.imaginary(), actual.imaginary(), TOLERANCE,
                () -> "Imaginary part mismatch: expected " + expected + " but was " + actual);
    }

    @Nested
    @DisplayName("Addition, subtraction and multiplication")
    class Arithmetic {

        @Test
        @DisplayName("(3 + 2i) + (1 + 4i) = 4 + 6i")
        void addsComponentWise() {
            Complex result = Complex.of(3, 2).add(Complex.of(1, 4));
            assertComplexEquals(Complex.of(4, 6), result);
        }

        @Test
        @DisplayName("(3 + 2i) - (1 + 4i) = 2 - 2i")
        void subtractsComponentWise() {
            Complex result = Complex.of(3, 2).subtract(Complex.of(1, 4));
            assertComplexEquals(Complex.of(2, -2), result);
        }

        @Test
        @DisplayName("(3 + 2i)(1 + 4i) = -5 + 14i, proving the sign from i squared")
        void multipliesUsingTheDistributiveRule() {
            Complex result = Complex.of(3, 2).multiply(Complex.of(1, 4));
            assertComplexEquals(Complex.of(-5, 14), result);
        }

        @Test
        @DisplayName("i * i = -1, the definition of the imaginary unit")
        void imaginaryUnitSquaredIsMinusOne() {
            Complex result = Complex.I.multiply(Complex.I);
            assertComplexEquals(Complex.of(-1, 0), result);
        }

        @Test
        @DisplayName("Multiplication is commutative")
        void multiplicationIsCommutative() {
            Complex z = Complex.of(3, 2);
            Complex w = Complex.of(1, 4);
            assertComplexEquals(z.multiply(w), w.multiply(z));
        }

        @Test
        @DisplayName("Multiplying by ONE leaves a number unchanged")
        void oneIsTheMultiplicativeIdentity() {
            Complex z = Complex.of(3, 2);
            assertComplexEquals(z, z.multiply(Complex.ONE));
        }
    }

    @Nested
    @DisplayName("Division")
    class Division {

        @Test
        @DisplayName("(3 + 2i) / (1 + 4i) = 11/17 - (10/17)i")
        void dividesByMultiplyingByTheConjugate() {
            Complex result = Complex.of(3, 2).divide(Complex.of(1, 4));
            assertComplexEquals(Complex.of(11.0 / 17.0, -10.0 / 17.0), result);
        }

        @Test
        @DisplayName("Dividing then multiplying by the same number recovers the original")
        void divisionIsTheInverseOfMultiplication() {
            Complex z = Complex.of(3, 2);
            Complex w = Complex.of(1, 4);
            assertComplexEquals(z, z.divide(w).multiply(w));
        }

        @Test
        @DisplayName("Dividing by zero throws rather than returning NaN")
        void divisionByZeroThrows() {
            Complex z = Complex.of(3, 2);
            assertThrows(ArithmeticException.class, () -> z.divide(Complex.ZERO));
        }
    }

    @Nested
    @DisplayName("Conjugate and magnitude")
    class ConjugateAndMagnitude {

        @Test
        @DisplayName("conj(3 + 2i) = 3 - 2i")
        void conjugateFlipsTheImaginarySign() {
            assertComplexEquals(Complex.of(3, -2), Complex.of(3, 2).conjugate());
        }

        @Test
        @DisplayName("z * conj(z) is real and equals the squared magnitude")
        void productWithConjugateIsReal() {
            Complex z = Complex.of(3, 2);
            Complex product = z.multiply(z.conjugate());

            assertEquals(0.0, product.imaginary(), TOLERANCE,
                    "The imaginary part must cancel exactly");
            assertEquals(z.magnitudeSquared(), product.real(), TOLERANCE,
                    "The real part must equal a^2 + b^2");
        }

        @Test
        @DisplayName("|3 + 4i| = 5, by Pythagoras")
        void magnitudeIsDistanceFromTheOrigin() {
            assertEquals(5.0, Complex.of(3, 4).magnitude(), TOLERANCE);
        }

        @Test
        @DisplayName("magnitudeSquared is the square of magnitude")
        void magnitudeSquaredAgreesWithMagnitude() {
            Complex z = Complex.of(3, 4);
            assertEquals(z.magnitude() * z.magnitude(), z.magnitudeSquared(), TOLERANCE);
        }

        @Test
        @DisplayName("ZERO has magnitude zero - the property the zero search relies on")
        void zeroHasZeroMagnitude() {
            assertEquals(0.0, Complex.ZERO.magnitude(), TOLERANCE);
        }
    }

    @Nested
    @DisplayName("Immutability and representation")
    class ImmutabilityAndRepresentation {

        @Test
        @DisplayName("Arithmetic returns new instances and leaves the operands untouched")
        void operationsDoNotMutateTheirOperands() {
            Complex z = Complex.of(3, 2);
            Complex w = Complex.of(1, 4);

            Complex sum = z.add(w);

            assertNotSame(z, sum, "add must return a new instance");
            assertEquals(3.0, z.real(), TOLERANCE, "the receiver must be unchanged");
            assertEquals(2.0, z.imaginary(), TOLERANCE, "the receiver must be unchanged");
            assertEquals(1.0, w.real(), TOLERANCE, "the argument must be unchanged");
            assertEquals(4.0, w.imaginary(), TOLERANCE, "the argument must be unchanged");
        }

        @Test
        @DisplayName("toString renders a + bi form for logs and failure messages")
        void toStringIsHumanReadable() {
            assertEquals("3.0 + 2.0i", Complex.of(3, 2).toString());
            assertEquals("3.0 - 2.0i", Complex.of(3, -2).toString());
        }
    }
}
