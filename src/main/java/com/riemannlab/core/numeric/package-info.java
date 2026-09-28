/**
 * Numerical building blocks shared across the project: series acceleration and root finding.
 *
 * <p>{@link com.riemannlab.core.numeric.AlternatingSeriesAccelerator} applies Euler transformation
 * to speed up convergence of a slowly-converging alternating series. The root finders
 * ({@link com.riemannlab.core.numeric.NewtonRootFinder}, {@link com.riemannlab.core.numeric.SecantRootFinder})
 * locate a complex root near a starting guess, returning a
 * {@link com.riemannlab.core.numeric.RootFindingResult} that reports both the root and how it was
 * found. This package has no knowledge of the zeta function itself, so it can be tested and
 * reasoned about independently.
 */
package com.riemannlab.core.numeric;