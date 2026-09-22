package com.riemannlab.zeros;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.core.numeric.ComplexRootFinder;
import com.riemannlab.core.numeric.RootFindingResult;
import com.riemannlab.core.numeric.SecantRootFinder;
import com.riemannlab.zeta.ZetaEvaluator;
import com.riemannlab.zeta.ZetaResult;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives a scanned candidate to machine precision with a root finder.
 *
 * <p>The scan locates a zero to about 1e-4 by fitting a line to three samples.
 * This class takes that estimate as a starting point and lets a root finder
 * close the remaining twelve orders of magnitude, which costs roughly eight
 * zeta evaluations per zero - trivial next to the hundreds the scan spent
 * finding the neighbourhood in the first place.</p>
 *
 * <p><strong>The search is unconstrained.</strong> The iterate may move
 * anywhere in the complex plane; nothing holds it to {@code Re(s) = 1/2}.
 * Pinning it there would guarantee a result on the critical line and prove
 * nothing, since the question being asked is precisely whether the zeros lie
 * there. Because the search is free, the real part of each answer is a
 * measurement.</p>
 *
 * <p>The secant method is the default, on the evidence measured in
 * {@code RootFinderComparisonTest}: Newton needs three zeta evaluations per
 * step to approximate a derivative, against the secant method's one, and loses
 * roughly two to one per digit gained despite its higher order.</p>
 *
 * <p><strong>The search stops at the evaluator's noise floor.</strong> A root
 * finder asked for more precision than the function evaluation carries will
 * spend iteration after iteration rearranging rounding error, and can stall
 * outright once the step it wants is smaller than one unit in the last place
 * of the height. So the target handed to the finder reports a value that is
 * indistinguishable from zero, in the sense of
 * {@link ZetaResult#isIndistinguishableFromZero()}, as exactly zero. The
 * residual reported afterwards is the true measured one, not the snapped
 * value.</p>
 *
 * <p>Immutable and thread-safe, given a thread-safe evaluator and finder.</p>
 */
public final class ZeroRefiner {

    private static final Logger log = LoggerFactory.getLogger(ZeroRefiner.class);

    private static final double CRITICAL_LINE_REAL_PART = 0.5;

    /**
     * Slack allowed on the evaluator's error bound when confirming a zero. The
     * bound is an estimate, not a guarantee, so an order of magnitude of room
     * avoids rejecting genuine zeros on an optimistically tight bound.
     */
    private static final double CONFIRMATION_SLACK = 10.0;

    private final ZetaEvaluator evaluator;
    private final ComplexRootFinder rootFinder;

    /**
     * Creates a refiner using the secant method.
     *
     * @param evaluator the zeta evaluator; must not be null
     */
    public ZeroRefiner(ZetaEvaluator evaluator) {
        this(evaluator, new SecantRootFinder());
    }

    /**
     * Creates a refiner with an explicit root finder.
     *
     * @param evaluator  the zeta evaluator; must not be null
     * @param rootFinder the root finder to drive; must not be null
     */
    public ZeroRefiner(ZetaEvaluator evaluator, ComplexRootFinder rootFinder) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
        this.rootFinder = Objects.requireNonNull(rootFinder, "rootFinder must not be null");
    }

    /**
     * Refines a scanned candidate, starting from its estimated height on the
     * critical line.
     *
     * @param candidate the candidate to refine; must not be null
     * @return the outcome, confirmed or not, never null
     */
    public RefinedZero refine(ZeroCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate must not be null");

        return refineFrom(
                Complex.of(CRITICAL_LINE_REAL_PART, candidate.estimatedHeight()));
    }

    /**
     * Refines every candidate, in order.
     *
     * @param candidates the candidates; must not be null
     * @return one result per candidate, in the same order
     */
    public List<RefinedZero> refineAll(List<ZeroCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");

        return candidates.stream().map(this::refine).toList();
    }

    /**
     * Refines from an arbitrary starting point, which need not lie on the
     * critical line.
     *
     * <p>This is what makes the off-line experiment possible: begin partway
     * across the critical strip and see where the search settles.</p>
     *
     * @param startingPoint where to begin; must not be null
     * @return the outcome, confirmed or not, never null
     */
    public RefinedZero refineFrom(Complex startingPoint) {
        Objects.requireNonNull(startingPoint, "startingPoint must not be null");

        AtomicInteger evaluationCount = new AtomicInteger();
        Function<Complex, Complex> target = s -> {
            evaluationCount.incrementAndGet();
            ZetaResult computed = evaluator.evaluate(s);

            // A magnitude smaller than the error of the computation that
            // produced it carries no information about where the zero is. Its
            // digits are rounding, not zeta. Reporting such a value as exactly
            // zero stops the root finder at the evaluator's noise floor rather
            // than letting it chase noise it cannot win against.
            //
            // Without this the finder is asked for a residual of 1e-14 at a
            // height where the evaluator's own bound is about 4e-14 - more
            // precision than exists - and the iterate eventually freezes
            // because the step it wants is smaller than ulp(t).
            return computed.isIndistinguishableFromZero()
                    ? Complex.ZERO
                    : computed.value();
        };

        RootFindingResult result;
        try {
            result = rootFinder.findRoot(target, startingPoint);
        } catch (IllegalArgumentException outsideDomain) {
            // The evaluator refuses Re(s) <= 0, where the eta series does not
            // converge. Reported rather than propagated: a search that wandered
            // out of the domain is a failed refinement, not a broken program.
            log.debug("Refinement from {} left the evaluator's domain: {}",
                    startingPoint, outsideDomain.getMessage());

            return new RefinedZero(startingPoint, startingPoint, 0.0, 0.0,
                    0, evaluationCount.get(), RefinedZero.Outcome.LEFT_DOMAIN);
        }

        return describe(startingPoint, result, evaluationCount.get());
    }

    private RefinedZero describe(
            Complex startingPoint, RootFindingResult result, int evaluations) {

        Complex location = result.root();

        ZetaResult finalValue;
        try {
            finalValue = evaluator.evaluate(location);
        } catch (IllegalArgumentException | ArithmeticException unusable) {
            log.debug("Final location {} could not be evaluated: {}",
                    location, unusable.getMessage());

            return new RefinedZero(startingPoint, location, 0.0, 0.0,
                    result.iterations(), evaluations + 1,
                    RefinedZero.Outcome.LEFT_DOMAIN);
        }

        double residual = finalValue.value().magnitude();
        double bound = finalValue.estimatedErrorBound();
        RefinedZero.Outcome outcome = classify(result, residual, bound);

        if (outcome != RefinedZero.Outcome.CONFIRMED) {
            log.debug("Refinement from {} ended at {} with outcome {}",
                    startingPoint, location, outcome);
        }

        return new RefinedZero(startingPoint, location, residual, bound,
                result.iterations(), evaluations + 1, outcome);
    }

    private RefinedZero.Outcome classify(
            RootFindingResult result, double residual, double bound) {

        if (!result.converged()) {
            return RefinedZero.Outcome.NOT_CONVERGED;
        }
        if (residual > CONFIRMATION_SLACK * bound) {
            return RefinedZero.Outcome.RESIDUAL_ABOVE_BOUND;
        }

        return RefinedZero.Outcome.CONFIRMED;
    }
}