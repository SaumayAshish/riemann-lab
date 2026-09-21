package com.riemannlab.core.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riemannlab.core.complex.Complex;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Runs the same contract against every {@link ComplexRootFinder}.
 *
 * <p>Every function here is a polynomial whose roots are known in closed form,
 * so a failure points at the algorithm and nothing else. The finders are built
 * and verified with no reference to the zeta function at all; pointing them at
 * zeta is a separate step, and keeping the two apart means a failure there
 * cannot be blamed on the arithmetic here.</p>
 */
class ComplexRootFinderContractTest {

    private static final double SQRT_TWO = 1.4142135623730951;

    /** {@code z^2 - 2}, real roots at plus and minus sqrt(2). */
    private static final Function<Complex, Complex> Z_SQUARED_MINUS_TWO =
            z -> z.multiply(z).subtract(Complex.ofReal(2.0));

    /** {@code z^2 + 1}, roots at plus and minus i. No real roots at all. */
    private static final Function<Complex, Complex> Z_SQUARED_PLUS_ONE =
            z -> z.multiply(z).add(Complex.ONE);

    /** {@code z^3 - 1}, roots at 1 and the two primitive cube roots of unity. */
    private static final Function<Complex, Complex> Z_CUBED_MINUS_ONE =
            z -> z.multiply(z).multiply(z).subtract(Complex.ONE);

    /** A constant with no root anywhere, and a derivative that is always zero. */
    private static final Function<Complex, Complex> CONSTANT_ONE = z -> Complex.ONE;

    static Stream<ComplexRootFinder> finders() {
        return Stream.of(new NewtonRootFinder(), new SecantRootFinder());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("Finds sqrt(2) as a root of z^2 - 2 to machine precision")
    void findsSquareRootOfTwo(ComplexRootFinder finder) {
        RootFindingResult result =
                finder.findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0));

        assertTrue(result.converged(),
                finder.name() + " did not converge: " + result.termination());
        assertEquals(SQRT_TWO, result.root().real(), 1e-13,
                finder.name() + " found " + result.root());
        assertEquals(0.0, result.root().imaginary(), 1e-13,
                "a real starting point on a real polynomial should stay real");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("Finds a purely imaginary root of z^2 + 1, which has no real roots")
    void findsImaginaryRoot(ComplexRootFinder finder) {
        RootFindingResult result =
                finder.findRoot(Z_SQUARED_PLUS_ONE, Complex.of(0.4, 0.6));

        assertTrue(result.converged(),
                finder.name() + " did not converge: " + result.termination());
        assertEquals(0.0, result.root().real(), 1e-11,
                finder.name() + " found " + result.root() + ", expected +/- i");
        assertEquals(1.0, Math.abs(result.root().imaginary()), 1e-11,
                finder.name() + " found " + result.root() + ", expected +/- i");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("Finds a primitive cube root of unity from a nearby complex start")
    void findsCubeRootOfUnity(ComplexRootFinder finder) {
        RootFindingResult result =
                finder.findRoot(Z_CUBED_MINUS_ONE, Complex.of(-0.4, 0.85));

        assertTrue(result.converged(),
                finder.name() + " did not converge: " + result.termination());
        assertEquals(1.0, result.root().magnitude(), 1e-11,
                "every cube root of unity lies on the unit circle");
        assertTrue(result.root().imaginary() > 0.5,
                finder.name() + " found " + result.root()
                        + ", expected the root in the upper half plane");
        assertEquals(-0.5, result.root().real(), 1e-11);
        assertEquals(Math.sqrt(3.0) / 2.0, result.root().imaginary(), 1e-11);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("The residual at the returned root is tiny")
    void residualAtTheRootIsTiny(ComplexRootFinder finder) {
        RootFindingResult result =
                finder.findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0));

        assertTrue(result.residualMagnitude() < 1e-12,
                finder.name() + " reported residual " + result.residualMagnitude());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("The iterate history starts at the guess and ends at the root")
    void iterateHistoryIsComplete(ComplexRootFinder finder) {
        Complex guess = Complex.ofReal(3.0);
        RootFindingResult result = finder.findRoot(Z_SQUARED_MINUS_TWO, guess);

        assertEquals(guess.real(), result.iterates().get(0).real(), 1e-15,
                "the first iterate must be the caller's guess");
        assertEquals(result.root().real(),
                result.iterates().get(result.iterates().size() - 1).real(), 0.0,
                "the last iterate must be the reported root");
        assertTrue(result.iterations() >= 1, "some work should have been done");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("Converges in a handful of iterations, not dozens")
    void convergesQuickly(ComplexRootFinder finder) {
        RootFindingResult result =
                finder.findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(3.0));

        assertTrue(result.iterations() <= 20,
                finder.name() + " took " + result.iterations() + " iterations");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("A function with no usable slope is reported, not divided by")
    void reportsVanishedDerivative(ComplexRootFinder finder) {
        RootFindingResult result = finder.findRoot(CONSTANT_ONE, Complex.ofReal(1.0));

        assertFalse(result.converged(), finder.name() + " claimed to solve 1 = 0");
        assertEquals(RootFindingResult.Termination.DERIVATIVE_VANISHED,
                result.termination(),
                finder.name() + " should notice the slope is zero");
        assertTrue(Double.isFinite(result.root().real())
                        && Double.isFinite(result.root().imaginary()),
                "the reported point must still be finite, was " + result.root());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("Starting exactly on a root terminates immediately")
    void startingOnTheRootTerminatesAtOnce(ComplexRootFinder finder) {
        RootFindingResult result =
                finder.findRoot(Z_SQUARED_MINUS_TWO, Complex.ofReal(SQRT_TWO));

        assertTrue(result.converged());
        assertTrue(result.iterations() <= 1,
                finder.name() + " took " + result.iterations()
                        + " iterations from an exact root");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("Results are deterministic")
    void findingIsDeterministic(ComplexRootFinder finder) {
        Complex guess = Complex.of(-0.4, 0.85);

        RootFindingResult first = finder.findRoot(Z_CUBED_MINUS_ONE, guess);
        RootFindingResult second = finder.findRoot(Z_CUBED_MINUS_ONE, guess);

        assertEquals(first.root().real(), second.root().real(), 0.0);
        assertEquals(first.root().imaginary(), second.root().imaginary(), 0.0);
        assertEquals(first.iterations(), second.iterations());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("Every finder identifies itself")
    void everyFinderHasAName(ComplexRootFinder finder) {
        assertTrue(finder.name() != null && !finder.name().isBlank());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("finders")
    @DisplayName("Null arguments are rejected")
    void rejectsNullArguments(ComplexRootFinder finder) {
        assertThrows(NullPointerException.class,
                () -> finder.findRoot(null, Complex.ONE));
        assertThrows(NullPointerException.class,
                () -> finder.findRoot(Z_SQUARED_MINUS_TWO, null));
    }
}
