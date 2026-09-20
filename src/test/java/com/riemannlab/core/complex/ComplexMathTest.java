package com.riemannlab.core.complex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies the complex exponential, logarithm and power against known values
 * and against the algebraic laws they must obey.
 *
 * <p>These three functions are what give meaning to {@code n^s} for a complex
 * exponent, which is the form every term of the Riemann zeta series takes.
 * An error here would corrupt every zeta evaluation in the project.</p>
 */
class ComplexMathTest {

    private static final double TOLERANCE = 1e-12;

    private static void assertComplexEquals(Complex expected, Complex actual) {
        assertEquals(expected.real(), actual.real(), TOLERANCE,
                () -> "Real part mismatch: expected " + expected + " but was " + actual);
        assertEquals(expected.imaginary(), actual.imaginary(), TOLERANCE,
                () -> "Imaginary part mismatch: expected " + expected + " but was " + actual);
    }

    @Nested
    @DisplayName("The complex exponential")
    class Exponential {

        @Test
        @DisplayName("exp(0) = 1")
        void exponentialOfZeroIsOne() {
            assertComplexEquals(Complex.ONE, ComplexMath.exp(Complex.ZERO));
        }

        @Test
        @DisplayName("exp(1) = e, matching the real exponential")
        void exponentialOfOneIsE() {
            assertComplexEquals(Complex.ofReal(Math.E), ComplexMath.exp(Complex.ONE));
        }

        @Test
        @DisplayName("Euler's identity: exp(i*pi) = -1")
        void eulersIdentityHolds() {
            Complex result = ComplexMath.exp(Complex.ofImaginary(Math.PI));
            assertComplexEquals(Complex.of(-1, 0), result);
        }

        @Test
        @DisplayName("exp(i*pi/2) = i, a quarter turn around the unit circle")
        void quarterTurnGivesTheImaginaryUnit() {
            Complex result = ComplexMath.exp(Complex.ofImaginary(Math.PI / 2));
            assertComplexEquals(Complex.I, result);
        }

        @Test
        @DisplayName("A purely imaginary exponent always lands on the unit circle")
        void purelyImaginaryExponentsHaveMagnitudeOne() {
            for (double angle : new double[] {0.0, 0.5, 1.0, 2.0, 3.0, -1.7, 10.0}) {
                double magnitude = ComplexMath.exp(Complex.ofImaginary(angle)).magnitude();
                assertEquals(1.0, magnitude, TOLERANCE,
                        "exp(i * " + angle + ") must have magnitude 1");
            }
        }

        @Test
        @DisplayName("exp turns addition into multiplication: exp(z + w) = exp(z) * exp(w)")
        void exponentialTurnsAdditionIntoMultiplication() {
            Complex z = Complex.of(0.5, 1.2);
            Complex w = Complex.of(-0.3, 2.1);

            Complex sumThenExp = ComplexMath.exp(z.add(w));
            Complex expThenProduct = ComplexMath.exp(z).multiply(ComplexMath.exp(w));

            assertComplexEquals(sumThenExp, expThenProduct);
        }

        @Test
        @DisplayName("The real part of the input controls magnitude, the imaginary part controls angle")
        void realPartSetsSizeAndImaginaryPartSetsDirection() {
            double a = 1.3;
            double b = 2.4;
            Complex result = ComplexMath.exp(Complex.of(a, b));

            assertEquals(Math.exp(a), result.magnitude(), TOLERANCE,
                    "magnitude must be e raised to the real part");
            assertEquals(b, Math.atan2(result.imaginary(), result.real()), TOLERANCE,
                    "angle must equal the imaginary part");
        }
    }

    @Nested
    @DisplayName("The complex logarithm")
    class Logarithm {

        @Test
        @DisplayName("log(1) = 0")
        void logarithmOfOneIsZero() {
            assertComplexEquals(Complex.ZERO, ComplexMath.log(Complex.ONE));
        }

        @Test
        @DisplayName("log(e) = 1, matching the real natural logarithm")
        void logarithmOfEIsOne() {
            assertComplexEquals(Complex.ONE, ComplexMath.log(Complex.ofReal(Math.E)));
        }

        @Test
        @DisplayName("log(i) = i*pi/2 - a quarter turn has angle pi/2 and magnitude 1")
        void logarithmOfTheImaginaryUnit() {
            assertComplexEquals(Complex.ofImaginary(Math.PI / 2), ComplexMath.log(Complex.I));
        }

        @Test
        @DisplayName("log(-1) = i*pi on the principal branch")
        void logarithmOfMinusOneUsesThePrincipalBranch() {
            assertComplexEquals(Complex.ofImaginary(Math.PI),
                    ComplexMath.log(Complex.ofReal(-1)));
        }

        @Test
        @DisplayName("The principal angle always lies in (-pi, pi]")
        void principalAngleStaysInRange() {
            Complex[] samples = {
                Complex.of(1, 1), Complex.of(-1, 1), Complex.of(-1, -1),
                Complex.of(1, -1), Complex.of(0, -5), Complex.of(-3, 0)
            };
            for (Complex z : samples) {
                double angle = ComplexMath.log(z).imaginary();
                assertEquals(true, angle > -Math.PI && angle <= Math.PI,
                        "angle of log(" + z + ") was " + angle + ", outside (-pi, pi]");
            }
        }

        @Test
        @DisplayName("log(0) throws rather than returning negative infinity")
        void logarithmOfZeroThrows() {
            assertThrows(ArithmeticException.class, () -> ComplexMath.log(Complex.ZERO));
        }
    }

    @Nested
    @DisplayName("The complex power")
    class Power {

        @Test
        @DisplayName("2^2 = 4, agreeing with ordinary arithmetic")
        void integerPowerMatchesOrdinaryArithmetic() {
            Complex result = ComplexMath.pow(Complex.ofReal(2), Complex.ofReal(2));
            assertComplexEquals(Complex.ofReal(4), result);
        }

        @Test
        @DisplayName("i^2 = -1")
        void imaginaryUnitSquared() {
            Complex result = ComplexMath.pow(Complex.I, Complex.ofReal(2));
            assertComplexEquals(Complex.of(-1, 0), result);
        }

        @Test
        @DisplayName("Anything to the power zero is one")
        void zeroExponentGivesOne() {
            assertComplexEquals(Complex.ONE, ComplexMath.pow(Complex.of(3, 2), Complex.ZERO));
        }

        @Test
        @DisplayName("2^-1 = 0.5")
        void negativeExponentInverts() {
            Complex result = ComplexMath.pow(Complex.ofReal(2), Complex.ofReal(-1));
            assertComplexEquals(Complex.ofReal(0.5), result);
        }

        @Test
        @DisplayName("|n^-s| depends only on Re(s) - the shape of every zeta term")
        void zetaTermMagnitudeDependsOnlyOnTheRealPartOfS() {
            double sigma = 0.5;
            double t = 14.134725141734693;
            Complex minusS = Complex.of(-sigma, -t);

            for (int n = 1; n <= 10; n++) {
                Complex term = ComplexMath.pow(Complex.ofReal(n), minusS);
                assertEquals(Math.pow(n, -sigma), term.magnitude(), TOLERANCE,
                        "term " + n + " must have magnitude n^-sigma regardless of t");
            }
        }

        @Test
        @DisplayName("Raising zero to a power throws, inheriting the logarithm's domain")
        void powerOfZeroThrows() {
            assertThrows(ArithmeticException.class,
                    () -> ComplexMath.pow(Complex.ZERO, Complex.ofReal(2)));
        }
    }

    @Nested
    @DisplayName("Round trips between exp and log")
    class RoundTrips {

        @Test
        @DisplayName("exp(log(z)) = z for any non-zero z")
        void expUndoesLog() {
            Complex[] samples = {
                Complex.of(3, 2), Complex.of(-1, 4), Complex.of(0.001, -0.002),
                Complex.of(-5, -5), Complex.ofReal(7), Complex.ofImaginary(-3)
            };
            for (Complex z : samples) {
                assertComplexEquals(z, ComplexMath.exp(ComplexMath.log(z)));
            }
        }

        @Test
        @DisplayName("log(exp(z)) = z when the imaginary part is inside the principal range")
        void logUndoesExpInsideThePrincipalBranch() {
            Complex z = Complex.of(0.7, 1.1);
            assertComplexEquals(z, ComplexMath.log(ComplexMath.exp(z)));
        }
    }
}
