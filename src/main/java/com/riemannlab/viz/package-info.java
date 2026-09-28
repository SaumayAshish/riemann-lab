/**
 * Rendering ζ(s) visually, backed by the same evaluation pipeline used everywhere else in the
 * project.
 *
 * <p>{@link com.riemannlab.viz.DomainColourRenderer} renders domain colouring across a region of
 * the complex plane defined by {@link com.riemannlab.viz.PlaneRegion}, using
 * {@link com.riemannlab.viz.PhasePalette} to map phase to colour.
 * {@link com.riemannlab.viz.CriticalLinePlotRenderer} plots |ζ(s)| along the critical line.
 * Neither renderer has its own separate evaluation logic — both call into the same {@code zeta}
 * package strategies used by the rest of the project, so a rendered image is never a claim that
 * hasn't also been checked elsewhere.
 */
package com.riemannlab.viz;