package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.complex.ComplexMath;

/**
 * the Dirichlet eta function,
 * {@code eta(s) = sum from n=1 to infinity of (-1)^(n-1) / n^s}, evaluated as
 * a partial sum over a finite number of terms.
 *
 * <p>This is the same series as {@link DirichletSeries} with alternating
 * signs, and that single change moves the region of convergence from
 * {@code Re(s) > 1} to {@code Re(s) > 0}. The new region contains the entire
 * critical strip, which is why this function rather than the defining series
 * is the computational basis of RiemannLab.</p>
 *
 * <p><strong>Accuracy, </strong> By the alternating series test, the error
 * after {@code N} terms is bounded by the size of term {@code N + 1}, which is
 * {@code N^-Re(s)}. On the critical line that is {@code 1/sqrt(N)}: a million
 * terms buys about three decimal places. Convergent but slow , which is enough
 * to see a zero and not enough to locate one.</p>
 */

public final class EtaFunction {

    private EtaFunction() {
        throw new AssertionError("EtaFunction is utility class and must not be instantiated");

    }

    /**
     * Sums the first {@code termCount} terms of the alternating series
     * {@code sum (-1)^(n-1) / n^s}.
     *
     * @param s
     * @param termCount
     * @return the partial sum after {@code termCount} is less than 1
     */
    public static Complex partialSum(Complex s, int termCount) {
        if (termCount < 1) {
            throw new IllegalArgumentException(
                    "termcount must be at least 1, but wes " + termCount);
        }
        Complex negatedExponent = Complex.ZERO.subtract(s);
        Complex sum = Complex.ZERO;

        for (int n = 1; n <= termCount; n++) {
            Complex term = ComplexMath.pow(Complex.ofReal(n), negatedExponent);
            boolean isOddTerm = (n % 2) == 1;
            sum = isOddTerm ? sum.add(term) : sum.subtract(term);
        }

        return sum;
    }

}
