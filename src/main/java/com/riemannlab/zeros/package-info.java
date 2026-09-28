/**
 * Locating and refining the non-trivial zeros of ζ(s) on the critical line.
 *
 * <p>{@link com.riemannlab.zeros.CriticalLineScanner} scans Re(s) = 1/2 for local minima of
 * |ζ(s)| — a cheap, approximate detection pass — producing
 * {@link com.riemannlab.zeros.ZeroCandidate} instances that {@link com.riemannlab.zeros.ZeroRefiner}
 * then refines to machine precision using the root finders from {@code core.numeric}.
 * {@link com.riemannlab.zeros.ScanMetricsReporter} and {@link com.riemannlab.zeros.ScanMetricsSummary}
 * turn the scanner's Micrometer metrics back into a human-readable summary.
 */
package com.riemannlab.zeros;