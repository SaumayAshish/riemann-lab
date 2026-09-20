package com.riemannlab.zeta;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.numeric.AlternatingSeriesAccelerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The production evaluator: the eta identity driven by Cohen-Rodriguez
 * Villegas-Zagier acceleration.
 *
 * <p>Reaches machine precision in a few dozen terms. The acceleration order is
 * chosen per point, because the error bound for oscillatory terms degrades by
 * roughly {@code e^(pi*|Im(s)|/2)} and must be paid for with extra terms.</p>
 *
 * <p>Immutable and therefore thread-safe, which matters for the parallel zero
 * search planned in a later phase.</p>
 */
public final class AcceleratedEtaEvaluator implements ZetaEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AcceleratedEtaEvaluator.class);

    private static final int DEFAULT_BASE_ORDER = 25;
    private static final double DEFAULT_ORDER_PER_UNIT_HEIGHT = 1.2;

    /**
     * Multiple of the machine epsilon used as a floor on the reported error.
     * Below this the algorithm's own truncation error is irrelevant, because
     * floating-point rounding dominates. Chosen empirically with margin.
     */
    private static final double ROUNDOFF_TERMS = 32.0;

    private final int baseOrder;
    private final double orderPerUnitHeight;

    /** Creates an evaluator with defaults suitable for machine precision. */
    public AcceleratedEtaEvaluator() {
        this(DEFAULT_BASE_ORDER, DEFAULT_ORDER_PER_UNIT_HEIGHT);
    }

    /**
     * Creates an evaluator with an explicit acceleration policy.
     *
     * @param baseOrder          acceleration order used at zero height
     * @param orderPerUnitHeight additional order per unit of {@code |Im(s)|}
     * @throws IllegalArgumentException if either value is not positive
     */
    public AcceleratedEtaEvaluator(int baseOrder, double orderPerUnitHeight) {
        if (baseOrder < 1) {
            throw new IllegalArgumentException("baseOrder must be at least 1, was " + baseOrder);
        }
        if (!(orderPerUnitHeight > 0.0)) {
            throw new IllegalArgumentException(
                    "orderPerUnitHeight must be positive, was " + orderPerUnitHeight);
        }

        this.baseOrder = baseOrder;
        this.orderPerUnitHeight = orderPerUnitHeight;

        log.info("Created {} (supports |Im(s)| up to {})", name(), maxSupportedHeight());
    }

    @Override
    public ZetaResult evaluate(Complex s) {
        Complex denominator = EtaIdentity.denominatorAt(s);
        int order = orderFor(s);

        Complex value = EtaFunction.acceleratedSum(s, order).divide(denominator);

        return new ZetaResult(value, order, errorBoundAt(s, order, denominator.magnitude()));
    }

    /**
     * Returns the acceleration order this evaluator would use at {@code s}.
     *
     * @param s the point of interest
     * @return the acceleration order
     * @throws IllegalArgumentException if the height exceeds what is supported
     */
    public int orderFor(Complex s) {
        double height = Math.abs(s.imaginary());
        long order = baseOrder + (long) Math.ceil(orderPerUnitHeight * height);

        if (order > AlternatingSeriesAccelerator.MAX_ORDER) {
            throw new IllegalArgumentException(
                    "height |Im(s)| = " + height + " exceeds the supported maximum of about "
                            + String.format("%.0f", maxSupportedHeight())
                            + ". Accurate evaluation at greater heights requires the"
                            + " Riemann-Siegel formula, which this project does not implement.");
        }

        return (int) order;
    }

    /**
     * The largest {@code |Im(s)|} this evaluator will accept.
     *
     * @return the height ceiling
     */
    public double maxSupportedHeight() {
        return (AlternatingSeriesAccelerator.MAX_ORDER - baseOrder) / orderPerUnitHeight;
    }

    @Override
    public String name() {
        return String.format("accelerated-eta(base=%d, perHeight=%.2f)",
                baseOrder, orderPerUnitHeight);
    }

    /**
     * Estimates the absolute error, taking the larger of two effects: the
     * algorithm's truncation error inflated by the oscillation penalty, and
     * the floor imposed by floating-point rounding. Division by the identity's
     * denominator scales both.
     */
    private double errorBoundAt(Complex s, int order, double denominatorMagnitude) {
        double height = Math.abs(s.imaginary());

        double truncation = AlternatingSeriesAccelerator.errorBound(order)
                * Math.exp(Math.PI * height / 2.0)
                * (1.0 + 2.0 * height);

        double roundoff = ROUNDOFF_TERMS * Math.ulp(1.0) * Math.sqrt(order);

        return Math.max(truncation, roundoff) / denominatorMagnitude;
    }
}