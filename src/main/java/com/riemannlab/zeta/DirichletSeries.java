package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.complex.ComplexMath;

/**
 * The defining Dirichlet series of the Riemann zeta function,
 * {@code zeta(s) = sum from n=1 to infinity of 1/n^s}, evaluated as a partial
 * sum over a finite number of terms.
 *
 * <p><strong>This class is not a usable zeta evaluator, and is not intended to
 * become one.</strong> It exists for two reasons: it is the definition the rest
 * of the project is ultimately approximating, and it serves as an independent
 * validation oracle for {@code Re(s)} comfortably above 1, where it does at
 * least converge to the right answer.</p>
 *
 * <p>Its limitations are mathematical, not implementation defects:</p>
 * <ul>
 *   <li>The series converges only when {@code Re(s) > 1}. For
 *       {@code Re(s) <= 1} - which includes the entire critical strip and
 *       therefore every non-trivial zero - the partial sums do not approach
 *       any value. On the critical line their magnitude grows like
 *       {@code sqrt(N)}.</li>
 *   <li>Where it does converge, the truncation error falls off only like
 *       {@code 1/N}. Reaching machine precision would require on the order of
 *       1e15 terms, by which point accumulated rounding error would exceed the
 *       remaining truncation error.</li>
 * </ul>
 *
 * <p>Both limitations are demonstrated empirically by
 * {@code com.riemannlab.app.cli.ConvergenceDemo}.</p>
 */
public final class DirichletSeries {

    private DirichletSeries() {
        throw new AssertionError("DirichletSeries is a utility class and must not be instantiated");
    }

    /**
     * Sums the first {@code termCount} terms of {@code sum 1/n^s}.
     *
     * <p>Terms are added in ascending order of {@code n}, which means the
     * largest contributions are accumulated first. That is the least accurate
     * ordering for floating-point summation, and it is kept deliberately: this
     * class documents the naive approach rather than optimising it.</p>
     *
     * <p>No convergence check is performed. Callers asking for
     * {@code Re(s) <= 1} receive a well-defined finite sum of the requested
     * terms, which is simply not an approximation of anything.</p>
     *
     * @param s         the complex exponent
     * @param termCount how many terms to sum; must be at least 1
     * @return the partial sum after {@code termCount} terms
     * @throws IllegalArgumentException if {@code termCount} is less than 1
     */
    public static Complex partialSum(Complex s, int termCount) {
        if (termCount < 1) {
            throw new IllegalArgumentException(
                    "termCount must be at least 1, but was " + termCount);
        }

        Complex negatedExponent = Complex.ZERO.subtract(s);
        Complex sum = Complex.ZERO;

        for (int n = 1; n <= termCount; n++) {
            Complex term = ComplexMath.pow(Complex.ofReal(n), negatedExponent);
            sum = sum.add(term);
        }

        return sum;
    }
}