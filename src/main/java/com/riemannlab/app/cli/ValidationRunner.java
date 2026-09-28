package com.riemannlab.app.cli;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.validation.PrecisionChecker;
import com.riemannlab.validation.PrecisionReport;
import com.riemannlab.validation.PrecisionSample;
import com.riemannlab.validation.ResidualReport;
import com.riemannlab.validation.ZetaAccuracyChecker;
import com.riemannlab.validation.ZetaAccuracyReport;
import com.riemannlab.validation.ZetaAgreementSample;
import com.riemannlab.zeros.CriticalLineScanner;
import com.riemannlab.zeros.RefinedZero;
import com.riemannlab.zeros.ZeroCandidate;
import com.riemannlab.zeros.ZeroRefiner;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import java.util.List;
import java.util.Map;

/**
 * Runs all three validation checks this project has built - refinement
 * outcomes, cross-evaluator agreement, and error-bound honesty - against
 * one live pass over the critical line, and prints them as a single
 * summary.
 *
 * <p>This is the last piece of the validation package. Nothing here
 * computes anything new; it only runs the existing pipeline once and hands
 * the results to {@link ResidualReport}, {@link ZetaAccuracyReport} and
 * {@link PrecisionReport}.</p>
 */
public final class ValidationRunner {

    private static final double SCAN_START = 1.0;
    private static final double SCAN_END = 55.0;
    private static final double SCAN_STEP = 0.1;
    private static final double RELATIVE_AGREEMENT_TOLERANCE = 1e-10;

    private static final AcceleratedEtaEvaluator SERIES = new AcceleratedEtaEvaluator();
    private static final ContinuedZetaEvaluator CONTINUED = new ContinuedZetaEvaluator(SERIES);
    private static final CriticalLineScanner SCANNER = new CriticalLineScanner(SERIES);
    private static final ZeroRefiner REFINER = new ZeroRefiner(SERIES);

    private static final List<Complex> AGREEMENT_SAMPLE_POINTS = List.of(
            Complex.of(0.25, 3.0),
            Complex.of(0.10, 7.0),
            Complex.of(0.30, -5.0),
            Complex.of(0.40, 20.0),
            Complex.of(0.45, 30.0));

    private ValidationRunner() {
        throw new AssertionError("ValidationRunner is an entry point and must not be instantiated");
    }
    /**
     * Runs the validation checks against known zeta values and known zeros.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        printHeader();

        ResidualReport residualReport = buildResidualReport();
        ZetaAccuracyReport accuracyReport = buildAccuracyReport();
        PrecisionReport precisionReport = buildPrecisionReport();

        printResidualSection(residualReport);
        printAccuracySection(accuracyReport);
        printPrecisionSection(precisionReport);
        printOverallVerdict(residualReport, accuracyReport, precisionReport);
        printDisclaimer();
    }

    private static ResidualReport buildResidualReport() {
        List<ZeroCandidate> candidates = SCANNER.scanForZeros(SCAN_START, SCAN_END, SCAN_STEP);
        List<RefinedZero> refined = REFINER.refineAll(candidates);
        return new ResidualReport(refined);
    }

    private static ZetaAccuracyReport buildAccuracyReport() {
        List<ZetaAgreementSample> samples =
                ZetaAccuracyChecker.check(SERIES, CONTINUED, AGREEMENT_SAMPLE_POINTS);
        return new ZetaAccuracyReport(samples);
    }

    private static PrecisionReport buildPrecisionReport() {
        List<PrecisionSample> samples =
                PrecisionChecker.check(CONTINUED, PrecisionChecker.NEGATIVE_ODD_INTEGER_REFERENCES);
        return new PrecisionReport(samples);
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("RiemannLab - validation summary");
        System.out.println("=".repeat(92));
        System.out.printf("scan: t in [%.1f, %.1f], step %.2f%n", SCAN_START, SCAN_END, SCAN_STEP);
        System.out.println("Three independent checks, run once each: did refinement converge, do two");
        System.out.println("unrelated evaluators agree, and can each evaluator's own error bound be");
        System.out.println("trusted.");
    }

    private static void printResidualSection(ResidualReport report) {
        section("1. Zero refinement outcomes");
        System.out.printf("%d candidate(s) refined%n", report.total());

        Map<RefinedZero.Outcome, Long> counts = report.outcomeCounts();
        for (RefinedZero.Outcome outcome : RefinedZero.Outcome.values()) {
            System.out.printf("  %-24s %d%n", outcome, counts.get(outcome));
        }

        System.out.println();
        System.out.printf("worst confirmed residual:        %.3e%n", report.worstConfirmedResidual());
        System.out.printf("worst confirmed residual ratio:  %.3f%n", report.worstConfirmedResidualRatio());
        System.out.printf("worst deviation from Re(s)=0.5:  %.3e%n", report.worstDeviationFromCriticalLine());
    }

    private static void printAccuracySection(ZetaAccuracyReport report) {
        section("2. Cross-evaluator agreement (eta series vs. functional-equation reflection)");
        System.out.printf("%d point(s) checked%n", report.count());
        System.out.printf("worst absolute difference: %.3e%n", report.worstAbsoluteDifference());
        System.out.printf("mean absolute difference:  %.3e%n", report.meanAbsoluteDifference());
        System.out.printf("worst relative difference: %.3e%n", report.worstRelativeDifference());
    }

    private static void printPrecisionSection(PrecisionReport report) {
        section("3. Error-bound honesty (against known closed-form values)");
        System.out.printf("%d known value(s) checked%n", report.count());
        System.out.printf("worst error/bound ratio:       %.3f%n", report.worstErrorRatio());
        System.out.printf("samples exceeding their bound: %d%n", report.countExceedingClaimedBound());
    }

    private static void printOverallVerdict(
            ResidualReport residualReport, ZetaAccuracyReport accuracyReport, PrecisionReport precisionReport) {
        section("Overall");

        boolean everyCandidateConfirmed =
                residualReport.countOf(RefinedZero.Outcome.CONFIRMED) == residualReport.total();
        boolean evaluatorsAgree = accuracyReport.worstRelativeDifference() < RELATIVE_AGREEMENT_TOLERANCE;
        boolean errorBoundsHeld = precisionReport.allWithinClaimedBound();

        printCheck("every scanned candidate confirmed as a zero", everyCandidateConfirmed);
        printCheck("independent evaluators agree to within " + RELATIVE_AGREEMENT_TOLERANCE, evaluatorsAgree);
        printCheck("every error bound held at the known values", errorBoundsHeld);

        boolean allPassed = everyCandidateConfirmed && evaluatorsAgree && errorBoundsHeld;
        System.out.println();
        System.out.println(allPassed
                ? "All three checks passed for this run."
                : "At least one check did not pass - see above for which.");
    }

    private static void printCheck(String description, boolean passed) {
        System.out.printf("  [%s] %s%n", passed ? "PASS" : "FAIL", description);
    }

    private static void printDisclaimer() {
        section("What this does and does not establish");
        System.out.println("Established, for this run only:");
        System.out.println("  - every zero candidate found in the scanned range converged under");
        System.out.println("    Newton's method to within its evaluator's own error bound");
        System.out.println("  - two independently-derived computations of zeta agree at the sampled");
        System.out.println("    points, to the tolerance checked above");
        System.out.println("  - the evaluator's claimed error bound was not exceeded at any of the");
        System.out.println("    known closed-form reference values");
        System.out.println();
        System.out.println("NOT established:");
        System.out.println("  - anything about zeros or points this run did not examine");
        System.out.println("  - that any evaluator is correct in general, only that it was consistent");
        System.out.println("    and honest on the checks performed here");
        System.out.println("  - the Riemann Hypothesis, in any part or degree");
        System.out.println();
        System.out.println("RiemannLab is a computational research and visualization project for");
        System.out.println("numerically evaluating the Riemann zeta function and investigating");
        System.out.println("its non-trivial zeros. Numerical experimentation is not proof.");
        System.out.println();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(92));
    }
}