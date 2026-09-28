/**
 * Strategies for evaluating the Riemann zeta function ζ(s) for complex s.
 *
 * <p>Three independent evaluators are provided — {@link com.riemannlab.zeta.NaiveEtaEvaluator} (a
 * direct Dirichlet eta series), {@link com.riemannlab.zeta.AcceleratedEtaEvaluator} (the same
 * series, accelerated via Euler transformation), and {@link com.riemannlab.zeta.ContinuedZetaEvaluator}
 * (analytic continuation via the functional equation) — implementing the common
 * {@link com.riemannlab.zeta.ZetaEvaluator} interface so callers can swap strategies, and so the
 * project can cross-check one evaluator's output against another's rather than trusting a single
 * implementation.
 */
package com.riemannlab.zeta;