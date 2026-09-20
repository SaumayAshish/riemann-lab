package com.riemannlab.core.complex;

/**
 * An immutable complex number of the form {@code real + imaginary * i},
 * where {@code i} is defined by {@code i * i == -1}.
 *
 * <p>This is the foundational value type of RiemannLab. The Riemann zeta
 * function maps complex numbers to complex numbers, so every evaluation,
 * every series term and every candidate zero is represented by an instance
 * of this type.</p>
 *
 * <p>Instances are immutable and therefore inherently thread-safe. Every
 * arithmetic method returns a new instance and never mutates its receiver
 * or its argument.</p>
 *
 * <p><strong>Numerical caveat:</strong> arithmetic is performed in IEEE-754
 * {@code double} precision, giving roughly 15.95 significant decimal digits.
 * Results are approximations. Never compare two instances for mathematical
 * equality with {@code equals}; compare component-wise within a tolerance.</p>
 *
 * @param real      the real part, Re(z)
 * @param imaginary the imaginary part, Im(z) - itself a real number
 */
public record Complex(double real, double imaginary) {

    /** The additive identity, {@code 0 + 0i}. */
    public static final Complex ZERO = new Complex(0.0, 0.0);

    /** The multiplicative identity, {@code 1 + 0i}. */
    public static final Complex ONE = new Complex(1.0, 0.0);

    /** The imaginary unit, {@code 0 + 1i}, satisfying {@code I.multiply(I) == -1}. */
    public static final Complex I = new Complex(0.0, 1.0);

    /**
     * Creates a complex number from its real and imaginary parts.
     *
     * @param real      the real part
     * @param imaginary the imaginary part
     * @return a new instance
     */
    public static Complex of(double real, double imaginary) {
        return new Complex(real, imaginary);
    }

    /**
     * Creates a complex number with no imaginary component, {@code real + 0i}.
     * Every real number is a complex number; this makes that explicit at call sites.
     *
     * @param real the real part
     * @return a new instance lying on the real axis
     */
    public static Complex ofReal(double real) {
        return new Complex(real, 0.0);
    }

    /**
     * Creates a purely imaginary number, {@code 0 + imaginary * i}.
     *
     * @param imaginary the imaginary part
     * @return a new instance lying on the imaginary axis
     */
    public static Complex ofImaginary(double imaginary) {
        return new Complex(0.0, imaginary);
    }

    /**
     * Adds another complex number to this one.
     *
     * <p>{@code (a + bi) + (c + di) = (a + c) + (b + d)i}</p>
     *
     * @param addend the number to add; must not be null
     * @return a new instance holding the sum
     */
    public Complex add(Complex addend) {
        return new Complex(
                this.real + addend.real,
                this.imaginary + addend.imaginary);
    }

    /**
     * Subtracts another complex number from this one.
     *
     * <p>{@code (a + bi) - (c + di) = (a - c) + (b - d)i}</p>
     *
     * @param subtrahend the number to subtract; must not be null
     * @return a new instance holding the difference
     */
    public Complex subtract(Complex subtrahend) {
        return new Complex(
                this.real - subtrahend.real,
                this.imaginary - subtrahend.imaginary);
    }

    /**
     * Multiplies this complex number by another.
     *
     * <p>{@code (a + bi)(c + di) = (ac - bd) + (ad + bc)i}</p>
     *
     * <p>The minus sign in the real part is a direct consequence of
     * {@code i * i == -1}. It is the most common source of error in
     * hand-written complex arithmetic.</p>
     *
     * @param multiplicand the number to multiply by; must not be null
     * @return a new instance holding the product
     */
    public Complex multiply(Complex multiplicand) {
        double productReal =
                this.real * multiplicand.real - this.imaginary * multiplicand.imaginary;
        double productImaginary =
                this.real * multiplicand.imaginary + this.imaginary * multiplicand.real;
        return new Complex(productReal, productImaginary);
    }

    /**
     * Divides this complex number by another.
     *
     * <p>Division is performed by multiplying numerator and denominator by the
     * conjugate of the divisor, which makes the denominator real:</p>
     *
     * <p>{@code (a + bi) / (c + di) = [(ac + bd) + (bc - ad)i] / (c^2 + d^2)}</p>
     *
     * <p><strong>Known limitation.</strong> Computing {@code c^2 + d^2} directly
     * overflows to infinity when {@code |c|} or {@code |d|} exceeds roughly
     * 1.3e154, and underflows to zero when both are smaller than roughly 1e-162 -
     * in which case this method reports a division by zero even though the
     * division is mathematically well defined. Smith's algorithm (1962) avoids
     * both by scaling first. The zeta evaluations in this project operate far
     * from those extremes, so the simpler and more readable form is used
     * deliberately. Revisit this if the limitation is ever reached in practice.</p>
     *
     * @param divisor the number to divide by; must not be null
     * @return a new instance holding the quotient
     * @throws ArithmeticException if the divisor is exactly zero
     */
    public Complex divide(Complex divisor) {
        double denominator =
                divisor.real * divisor.real + divisor.imaginary * divisor.imaginary;

        if (denominator == 0.0) {
            throw new ArithmeticException(
                    "Cannot divide " + this + " by zero (divisor was " + divisor + ")");
        }

        double quotientReal =
                (this.real * divisor.real + this.imaginary * divisor.imaginary) / denominator;
        double quotientImaginary =
                (this.imaginary * divisor.real - this.real * divisor.imaginary) / denominator;

        return new Complex(quotientReal, quotientImaginary);
    }

    /**
     * Returns the complex conjugate, {@code a - bi}, which is this number
     * reflected across the real axis.
     *
     * <p>The defining property is that {@code z.multiply(z.conjugate())} is
     * always real and equal to {@code z.magnitudeSquared()}.</p>
     *
     * @return a new instance holding the conjugate
     */
    public Complex conjugate() {
        return new Complex(this.real, -this.imaginary);
    }

    /**
     * Returns the magnitude (modulus) of this number: its distance from the
     * origin of the complex plane, {@code sqrt(a^2 + b^2)}.
     *
     * <p>This is the project's primary measure of "how close to zero is this
     * value". A complex number is zero if and only if its magnitude is zero,
     * which reduces a two-dimensional test to a single non-negative number.</p>
     *
     * <p>{@link Math#hypot} is used rather than {@code Math.sqrt(a * a + b * b)}
     * because it computes the result without overflowing on the intermediate
     * squares.</p>
     *
     * @return the magnitude, always non-negative
     */
    public double magnitude() {
        return Math.hypot(this.real, this.imaginary);
    }

    /**
     * Returns the squared magnitude, {@code a^2 + b^2}, without taking a
     * square root.
     *
     * <p>Comparing {@code magnitude() < tolerance} is equivalent to comparing
     * {@code magnitudeSquared() < tolerance * tolerance}, so this method allows
     * magnitude comparisons in hot loops while avoiding a square root per call.
     * Unlike {@link #magnitude()} it offers no overflow protection.</p>
     *
     * @return the squared magnitude, always non-negative
     */
    public double magnitudeSquared() {
        return this.real * this.real + this.imaginary * this.imaginary;
    }

    /**
     * Returns a human-readable form such as {@code "3.0 + 2.0i"} or
     * {@code "3.0 - 2.0i"}, overriding the record default so that log output
     * and test failure messages read as mathematics.
     *
     * @return this number in {@code a + bi} form
     */
    @Override
    public String toString() {
        return this.imaginary < 0
                ? this.real + " - " + (-this.imaginary) + "i"
                : this.real + " + " + this.imaginary + "i";
    }
}
