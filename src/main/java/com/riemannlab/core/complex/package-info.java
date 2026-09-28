/**
 * Complex-number value type and complex-valued elementary functions.
 *
 * <p>{@link com.riemannlab.core.complex.Complex} is the immutable value type (a record) used
 * everywhere a complex number is passed around in this project — zeta evaluation, root finding,
 * gamma function computation, and rendering. {@link com.riemannlab.core.complex.ComplexMath}
 * provides complex-valued exp, log, and pow, since {@code java.lang.Math} only operates on real
 * numbers.
 */
package com.riemannlab.core.complex;