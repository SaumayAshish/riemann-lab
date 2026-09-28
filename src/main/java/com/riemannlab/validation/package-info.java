/**
 * Cross-checks that keep this project honest about what it has and hasn't actually verified.
 *
 * <p>{@link com.riemannlab.validation.KnownZeroCatalog} and
 * {@link com.riemannlab.validation.KnownZetaValue} hold reference data.
 * {@link com.riemannlab.validation.PrecisionChecker} and
 * {@link com.riemannlab.validation.ZetaAccuracyChecker} check whether a claimed error bound
 * matches the actual residual, and whether independent evaluators agree with each other — three
 * separate validation passes rather than one, because a single passing check is not strong
 * evidence on its own.
 */
package com.riemannlab.validation;