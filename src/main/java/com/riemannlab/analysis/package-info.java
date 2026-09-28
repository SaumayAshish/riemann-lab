/**
 * Theoretical zero-density and spacing tables, independent of the live scanner.
 *
 * <p>{@link com.riemannlab.analysis.ZeroSpacingTable} and {@link com.riemannlab.analysis.ZeroSpacing}
 * work from known reference data rather than a live scan, giving a baseline to compare the
 * scanner's actual output against. {@link com.riemannlab.analysis.CriticalLineSampler} and
 * {@link com.riemannlab.analysis.CriticalLineSample} sample |ζ(s)| along the critical line for
 * that comparison.
 */
package com.riemannlab.analysis;