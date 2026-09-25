package com.riemannlab.zeros;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.ZetaEvaluator;
import com.riemannlab.zeta.ZetaResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Sweeps the critical line {@code Re(s) = 1/2} looking for local minima of
 * {@code |zeta|}, each of which is a region that may contain a zero.
 *
 * <p>Detection is kept separate from refinement on purpose. This class is
 * cheap per point and makes no attempt at precision; its only obligations are
 * to visit the whole range and not to miss anything. Driving a candidate to
 * machine precision is a different algorithm with different failure modes.</p>
 *
 * <p><strong>Why local minima rather than sign changes.</strong>
 * {@code |zeta|} is never negative, so a zero is a point where the curve
 * touches the axis rather than crossing it. The intermediate value theorem,
 * the usual tool for bracketing a root, does not apply. A strict local minimum
 * on the grid is the available evidence.</p>
 *
 * <p><strong>A minimum is not a zero.</strong> {@code |zeta(0.5 + it)|} has a
 * genuine local minimum near t = 2.4 where the magnitude falls to about 0.53
 * and rises again, while the first actual zero is at 14.13. {@link #scan}
 * reports every minimum, which is the honest raw measurement;
 * {@link #scanForZeros} keeps only those consistent with a zero.</p>
 *
 * <p><strong>Completeness is not guaranteed.</strong> Two zeros lying closer
 * together than the step can cancel out of the local-minimum test entirely,
 * and neither will be reported. The average spacing near height {@code T} is
 * about {@code 2*pi/ln(T/(2*pi))}, which shrinks without bound as {@code T}
 * grows, so any fixed step eventually becomes too coarse. This class warns
 * when the step looks too large for the range, but proving that no zero was
 * missed requires counting zeros independently - Turing's method - which this
 * project does not implement.</p>
 *
 * <p><strong>Sequential vs. parallel.</strong> {@link #scan} and
 * {@link #scanForZeros} evaluate one sample at a time. {@link #scanParallel}
 * and {@link #scanForZerosParallel} evaluate all samples concurrently, then
 * run the identical local-minimum detection sequentially over the results -
 * detection is cheap and order-dependent, evaluation is expensive and
 * independent per point. Both pairs are proven to agree exactly on the same
 * input; see the correctness test.</p>
 *
 * <p>Immutable and thread-safe, given a thread-safe evaluator.</p>
 */
public final class CriticalLineScanner {

    private static final Logger log = LoggerFactory.getLogger(CriticalLineScanner.class);

    private static final double CRITICAL_LINE_REAL_PART = 0.5;

    /** Height below which the spacing formula stops being meaningful. */
    private static final double MINIMUM_MEANINGFUL_HEIGHT = 10.0;

    /** How many samples to place across one average gap between zeros. */
    private static final double SAMPLES_PER_ZERO_GAP = 8.0;

    /** A step coarser than spacing divided by this is reported as risky. */
    private static final double RISKY_STEP_DIVISOR = 4.0;

    private final ZetaEvaluator evaluator;

    /**
     * Creates a scanner driven by the given evaluator.
     *
     * @param evaluator the evaluator to sample with; must not be null
     */
    public CriticalLineScanner(ZetaEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    }

    /**
     * Scans a range of heights and returns every strict local minimum of
     * {@code |zeta(0.5 + it)|} found on the grid.
     *
     * <p>No plausibility judgement is applied. Some of these minima are
     * features of the curve rather than evidence of zeros; see
     * {@link #scanForZeros}.</p>
     *
     * @param startHeight the lowest height to sample; must be positive
     * @param endHeight   the highest height to sample; must exceed startHeight
     * @param step        the sample spacing; must be positive and fit in the range
     * @return the minima, in increasing order of height, never null
     * @throws IllegalArgumentException if the range or step is unusable
     */
    public List<ZeroCandidate> scan(double startHeight, double endHeight, double step) {
        MDC.put("scanMode", "sequential");
        try {
            validate(startHeight, endHeight, step);
            warnIfStepTooCoarse(endHeight, step);

            int sampleCount = (int) Math.round((endHeight - startHeight) / step);
            long startNanos = System.nanoTime();

            List<ZeroCandidate> candidates = new ArrayList<>();

            double magnitudeBefore = Double.NaN;
            double magnitudeAt = Double.NaN;
            double errorBoundAt = Double.NaN;

            for (int i = 0; i <= sampleCount; i++) {
                double t = startHeight + i * step;

                ZetaResult result = evaluator.evaluate(Complex.of(CRITICAL_LINE_REAL_PART, t));
                double magnitudeAfter = result.value().magnitude();

                boolean haveThreeSamples = i >= 2;
                boolean isStrictLocalMinimum = haveThreeSamples
                        && magnitudeAt < magnitudeBefore
                        && magnitudeAt < magnitudeAfter;

                if (isStrictLocalMinimum) {
                    ZeroCandidate candidate = new ZeroCandidate(
                            startHeight + (i - 1) * step,
                            magnitudeAt,
                            step,
                            magnitudeBefore,
                            magnitudeAfter,
                            errorBoundAt);

                    candidates.add(candidate);
                    log.debug("Local minimum at t ~ {}, drop {}",
                            candidate.estimatedHeight(), candidate.relativeDrop());
                }

                magnitudeBefore = magnitudeAt;
                magnitudeAt = magnitudeAfter;
                errorBoundAt = result.estimatedErrorBound();
            }

            log.info("Scanned t in [{}, {}] step {} using {}: {} evaluations, {} minima, {} ms",
                    startHeight, endHeight, step, evaluator.name(),
                    sampleCount + 1, candidates.size(),
                    (System.nanoTime() - startNanos) / 1_000_000);

            return List.copyOf(candidates);
        } finally {
            MDC.remove("scanMode");
        }
    }

    /**
     * Scans a range and returns only the candidates consistent with containing
     * a zero.
     *
     * <p>{@link #scan} reports every strict local minimum, which is the honest
     * raw measurement and useful for diagnostics. Not every local minimum is
     * near a zero: {@code |zeta(0.5 + it)|} dips to about 0.53 near t = 2.4
     * without approaching the axis at all. This method applies
     * {@link ZeroCandidate#looksLikeZero()} so callers searching for zeros are
     * not handed features of the curve.</p>
     *
     * @param startHeight the lowest height to sample
     * @param endHeight   the highest height to sample
     * @param step        the sample spacing
     * @return the plausible candidates, in increasing order of height
     * @throws IllegalArgumentException if the range or step is unusable
     */
    public List<ZeroCandidate> scanForZeros(double startHeight, double endHeight, double step) {
        return filterPlausible(scan(startHeight, endHeight, step));
    }

    /**
     * Scans a range exactly as {@link #scan} does, except that every sample
     * point is evaluated concurrently before the (sequential) local-minimum
     * detection runs over the results.
     *
     * <p>Evaluating a point is independent of evaluating any other point, so
     * that part parallelizes cleanly given a thread-safe evaluator. Detecting
     * a local minimum depends on the order of neighbouring samples, so that
     * part still runs sequentially, over the already-computed array - it is
     * cheap enough that parallelizing it would not help anyway.</p>
     *
     * <p>Produces exactly the same candidates, in exactly the same order, as
     * {@link #scan} given the same arguments and evaluator - proven by a
     * dedicated correctness test rather than assumed.</p>
     *
     * @param startHeight the lowest height to sample; must be positive
     * @param endHeight   the highest height to sample; must exceed startHeight
     * @param step        the sample spacing; must be positive and fit in the range
     * @return the minima, in increasing order of height, never null
     * @throws IllegalArgumentException if the range or step is unusable
     */
    public List<ZeroCandidate> scanParallel(double startHeight, double endHeight, double step) {
        MDC.put("scanMode", "parallel");
        try {
            validate(startHeight, endHeight, step);
            warnIfStepTooCoarse(endHeight, step);

            int sampleCount = (int) Math.round((endHeight - startHeight) / step);
            long startNanos = System.nanoTime();

            ZetaResult[] results = IntStream.rangeClosed(0, sampleCount)
                    .parallel()
                    .mapToObj(i -> evaluator.evaluate(
                            Complex.of(CRITICAL_LINE_REAL_PART, startHeight + i * step)))
                    .toArray(ZetaResult[]::new);

            List<ZeroCandidate> candidates = new ArrayList<>();

            for (int c = 1; c < sampleCount; c++) {
                double magnitudeBefore = results[c - 1].value().magnitude();
                double magnitudeAt = results[c].value().magnitude();
                double magnitudeAfter = results[c + 1].value().magnitude();

                boolean isStrictLocalMinimum = magnitudeAt < magnitudeBefore
                        && magnitudeAt < magnitudeAfter;

                if (isStrictLocalMinimum) {
                    ZeroCandidate candidate = new ZeroCandidate(
                            startHeight + c * step,
                            magnitudeAt,
                            step,
                            magnitudeBefore,
                            magnitudeAfter,
                            results[c].estimatedErrorBound());

                    candidates.add(candidate);
                    log.debug("Local minimum at t ~ {}, drop {}",
                            candidate.estimatedHeight(), candidate.relativeDrop());
                }
            }

            log.info("Scanned (parallel) t in [{}, {}] step {} using {}: {} evaluations, {} minima, {} ms",
                    startHeight, endHeight, step, evaluator.name(),
                    sampleCount + 1, candidates.size(),
                    (System.nanoTime() - startNanos) / 1_000_000);

            return List.copyOf(candidates);
        } finally {
            MDC.remove("scanMode");
        }
    }

    /**
     * The parallel counterpart to {@link #scanForZeros}: same plausibility
     * filter, applied to {@link #scanParallel}'s output.
     *
     * @param startHeight the lowest height to sample
     * @param endHeight   the highest height to sample
     * @param step        the sample spacing
     * @return the plausible candidates, in increasing order of height
     * @throws IllegalArgumentException if the range or step is unusable
     */
    public List<ZeroCandidate> scanForZerosParallel(double startHeight, double endHeight, double step) {
        return filterPlausible(scanParallel(startHeight, endHeight, step));
    }

    /**
     * The average gap between consecutive non-trivial zeros near a height.
     *
     * <p>Derived from the zero-counting estimate
     * {@code N(T) ~ (T/2pi)*ln(T/2pi) - T/2pi}: differentiating gives the
     * density, and the reciprocal is the spacing,
     * {@code 2*pi/ln(T/(2*pi))}.</p>
     *
     * <p>The formula degenerates as the height approaches {@code 2*pi}, where
     * the logarithm vanishes, so below a floor it returns the value at that
     * floor. There are no zeros down there anyway; the first is at 14.13.</p>
     *
     * @param height the height of interest
     * @return the average spacing, always positive and finite
     */
    public static double averageZeroSpacing(double height) {
        double effectiveHeight = Math.max(Math.abs(height), MINIMUM_MEANINGFUL_HEIGHT);
        return 2.0 * Math.PI / Math.log(effectiveHeight / (2.0 * Math.PI));
    }

    /**
     * A step small enough to resolve neighbouring zeros near a height.
     *
     * @param height the highest height to be scanned
     * @return a suggested step size
     */
    public static double recommendedStep(double height) {
        return averageZeroSpacing(height) / SAMPLES_PER_ZERO_GAP;
    }

    private List<ZeroCandidate> filterPlausible(List<ZeroCandidate> allMinima) {
        List<ZeroCandidate> plausible = allMinima.stream()
                .filter(ZeroCandidate::looksLikeZero)
                .toList();

        int discarded = allMinima.size() - plausible.size();
        if (discarded > 0) {
            log.info("Discarded {} of {} local minima as too shallow to contain a zero",
                    discarded, allMinima.size());
        }

        return plausible;
    }

    private void validate(double startHeight, double endHeight, double step) {
        if (!(startHeight > 0.0)) {
            throw new IllegalArgumentException(
                    "startHeight must be positive, was " + startHeight);
        }
        if (!(endHeight > startHeight)) {
            throw new IllegalArgumentException(
                    "endHeight (" + endHeight + ") must exceed startHeight (" + startHeight + ")");
        }
        if (!(step > 0.0)) {
            throw new IllegalArgumentException("step must be positive, was " + step);
        }
        if (step > (endHeight - startHeight) / 2.0) {
            throw new IllegalArgumentException(
                    "step " + step + " is too large for the range ["
                            + startHeight + ", " + endHeight
                            + "]; at least three samples are needed to detect a minimum");
        }
    }

    private void warnIfStepTooCoarse(double endHeight, double step) {
        double spacing = averageZeroSpacing(endHeight);

        if (step > spacing / RISKY_STEP_DIVISOR) {
            log.warn("Step {} is coarse for heights near {}: average zero spacing there is"
                            + " about {}. Zeros closer together than the step may be missed"
                            + " silently. Consider {}.",
                    step, endHeight, String.format("%.3f", spacing),
                    String.format("%.3f", recommendedStep(endHeight)));
        }
    }
}