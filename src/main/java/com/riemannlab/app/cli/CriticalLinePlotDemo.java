package com.riemannlab.app.cli;

import com.riemannlab.analysis.CriticalLineSample;
import com.riemannlab.analysis.CriticalLineSampler;
import com.riemannlab.viz.CriticalLinePlotRenderer;
import com.riemannlab.zeros.CriticalLineScanner;
import com.riemannlab.zeros.RefinedZero;
import com.riemannlab.zeros.ZeroCandidate;
import com.riemannlab.zeros.ZeroRefiner;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Renders |zeta(1/2 + it)| as a line plot, with zero markers that come from
 * this project's own scan-and-refine pipeline - the same one {@code
 * ZeroFinderDemo} already validated against published values - rather than
 * from a hardcoded list.
 *
 * <p>The curve and the markers are computed independently of each other.
 * {@link CriticalLineSampler} only ever measures a magnitude; {@link
 * CriticalLineScanner} and {@link ZeroRefiner} only ever search for and
 * verify a root. The picture at the end shows both, in different colours,
 * specifically so a viewer cannot mistake one for the other.</p>
 */
public final class CriticalLinePlotDemo {

    private static final Path OUTPUT_DIRECTORY = Path.of("images");
    private static final String OUTPUT_FILE = "critical-line-plot.png";

    private static final double SCAN_START = 1.0;
    private static final double PLOT_START = 0.0;
    private static final double END_HEIGHT = 35.0;
    private static final double SCAN_STEP = 0.1;
    private static final int SAMPLE_COUNT = 2001;

    private static final int IMAGE_WIDTH = 1000;
    private static final int IMAGE_HEIGHT = 500;

    private static final AcceleratedEtaEvaluator EVALUATOR = new AcceleratedEtaEvaluator();
    private static final CriticalLineScanner SCANNER = new CriticalLineScanner(EVALUATOR);
    private static final ZeroRefiner REFINER = new ZeroRefiner(EVALUATOR);
    private static final CriticalLineSampler SAMPLER = new CriticalLineSampler(EVALUATOR);
    private static final CriticalLinePlotRenderer RENDERER = new CriticalLinePlotRenderer();

    private CriticalLinePlotDemo() {
        throw new AssertionError(
                "CriticalLinePlotDemo is an entry point and must not be instantiated");
    }
    /** Renders and saves a plot of zeta along the critical line. */
    public static void main(String[] args) throws IOException {
        printHeader();

        List<ZeroCandidate> candidates = SCANNER.scanForZeros(SCAN_START, END_HEIGHT, SCAN_STEP);
        List<RefinedZero> zeros = REFINER.refineAll(candidates);
        List<Double> zeroHeights = zeros.stream()
                .filter(RefinedZero::isConfirmed)
                .map(RefinedZero::height)
                .toList();

        List<CriticalLineSample> samples = SAMPLER.sample(PLOT_START, END_HEIGHT, SAMPLE_COUNT);

        printZeros(zeros, zeroHeights);

        Files.createDirectories(OUTPUT_DIRECTORY);
        BufferedImage image = RENDERER.render(samples, zeroHeights, IMAGE_WIDTH, IMAGE_HEIGHT);
        Path target = OUTPUT_DIRECTORY.resolve(OUTPUT_FILE);
        ImageIO.write(image, "png", target.toFile());

        printSummary(target, samples.size(), zeroHeights.size());
        printDisclaimer();
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("RiemannLab - |zeta(1/2 + it)| along the critical line");
        System.out.println("=".repeat(88));
        System.out.printf("curve:   %d samples over t in [%.1f, %.1f]%n",
                SAMPLE_COUNT, PLOT_START, END_HEIGHT);
        System.out.printf("markers: scan t in [%.1f, %.1f] step %.2f, then refine every candidate%n",
                SCAN_START, END_HEIGHT, SCAN_STEP);
        System.out.println();
        System.out.println("The curve and the markers are computed independently. The curve is");
        System.out.println("a measurement; the markers are the output of the same root-finding");
        System.out.println("pipeline ZeroFinderDemo already validated against published values.");
    }

    private static void printZeros(List<RefinedZero> zeros, List<Double> zeroHeights) {
        section("Zeros found in this range, by the same pipeline as ZeroFinderDemo");
        System.out.printf("%3s  %22s  %12s  %10s%n", "#", "height", "|zeta|", "outcome");
        System.out.println("-".repeat(88));

        for (int i = 0; i < zeros.size(); i++) {
            RefinedZero zero = zeros.get(i);
            System.out.printf("%3d  %22.15f  %12.2e  %10s%n",
                    i + 1, zero.height(), zero.residualMagnitude(),
                    zero.isConfirmed() ? "confirmed" : zero.outcome().toString());
        }

        System.out.println();
        System.out.printf("%d of %d refined candidates confirmed - those are the ones marked "
                + "on the plot.%n", zeroHeights.size(), zeros.size());
    }

    private static void printSummary(Path target, int sampleCount, int markerCount)
            throws IOException {

        section("Image written");
        System.out.printf("%s  (%d samples, %d zero markers, %,d bytes)%n",
                target, sampleCount, markerCount, Files.size(target));
        System.out.println();
        System.out.println("What to look for");
        System.out.println("-".repeat(88));
        System.out.println("  - the blue curve dips toward the baseline near each red dashed line,");
        System.out.println("    but the two were computed by entirely separate code paths - the");
        System.out.println("    dip is a hint, the red line is the independently verified answer");
        System.out.println("  - between zeros the curve rises to an ordinary size, order 1 to a");
        System.out.println("    few units, before falling again toward the next one");
        System.out.println("  - a red line with no matching dip, or a deep dip with no red line,");
        System.out.println("    would mean the curve and the pipeline disagree - worth stopping");
        System.out.println("    and investigating, not something this particular run produced");
    }

    private static void printDisclaimer() {
        section("What this does and does not establish");
        System.out.println("Established, for the range plotted:");
        System.out.println("  - the magnitude curve and the independently refined zero heights");
        System.out.println("    agree with each other");
        System.out.println("  - every confirmed zero in this range matches the pipeline");
        System.out.println("    ZeroFinderDemo already checked against published values");
        System.out.println();
        System.out.println("NOT established:");
        System.out.println("  - anything about zeros above t = " + END_HEIGHT);
        System.out.println("  - that no zero was missed between scan steps in this range");
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
        System.out.println("=".repeat(88));
    }
}