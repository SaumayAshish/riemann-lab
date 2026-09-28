package com.riemannlab.app.cli;

import com.riemannlab.viz.DomainColourRenderer;
import com.riemannlab.viz.PlaneRegion;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Renders the zeta function as domain-coloured images and writes them to
 * {@code images/}.
 *
 * <p>Three views, chosen so that between them they show every feature the
 * earlier phases established:</p>
 *
 * <ul>
 *   <li><strong>The plane.</strong> The pole at {@code s = 1}, the trivial
 *       zeros marching left along the real axis, and the first non-trivial
 *       zeros at {@code +/-14.13i} - all in one picture, and all reached
 *       through the functional equation on the left-hand side.</li>
 *   <li><strong>The trivial zeros.</strong> A wide, shallow strip along the
 *       real axis. The zeros appear as a row of evenly spaced dark points,
 *       which is the analytic continuation made visible: none of those points
 *       is computable from the series this project started with.</li>
 *   <li><strong>One zero, close up.</strong> A square around
 *       {@code 0.5 + 14.13i} at a scale where the colour wheel wrapping once
 *       around the zero is unmistakable.</li>
 * </ul>
 *
 * <p>Every region is chosen with square pixels, which the renderer's own
 * {@code aspectDistortion} would report as exactly 1. A stretched plot is a
 * lie about the geometry of the function.</p>
 */
public final class DomainColourDemo {

    private static final Path OUTPUT_DIRECTORY = Path.of("images");

    private static final double FIRST_ZERO = 14.134725141734693;

    private static final DomainColourRenderer RENDERER =
            new DomainColourRenderer(new ContinuedZetaEvaluator());

    private DomainColourDemo() {
        throw new AssertionError("DomainColourDemo is an entry point and must not be instantiated");
    }
    /**
     * Renders and saves a domain-coloured plot of zeta over a region of the complex plane.
     *
     * @param args unused
     * @throws IOException if the image cannot be written to disk
     */
    public static void main(String[] args) throws IOException {
        System.out.println();
        System.out.println("RiemannLab - domain colouring");
        System.out.println("=".repeat(84));
        System.out.println("hue        = arg(zeta)   which way the value points");
        System.out.println("lightness  = |zeta|      black at a zero, white at the pole");
        System.out.println("bands      = powers of 2 count the rings to read the magnitude");
        System.out.println();

        Files.createDirectories(OUTPUT_DIRECTORY);

        write("zeta-plane.png",
                "the plane: pole, trivial zeros and the first non-trivial pair",
                new PlaneRegion(-12.0, 12.0, -18.0, 18.0, 800, 1200));

        write("zeta-trivial-zeros.png",
                "the trivial zeros along the real axis",
                new PlaneRegion(-13.0, 3.0, -4.0, 4.0, 1200, 600));

        write("zeta-first-zero.png",
                "one non-trivial zero, close up",
                new PlaneRegion(0.0, 1.0, FIRST_ZERO - 0.5, FIRST_ZERO + 0.5, 800, 800));

        System.out.println();
        System.out.println("What to look for");
        System.out.println("=".repeat(84));
        System.out.println("zeta-plane.png");
        System.out.println("  - one white point on the real axis at s = 1: the pole");
        System.out.println("  - dark points at s = -2, -4, -6, -8, -10: the trivial zeros");
        System.out.println("  - two dark points on the vertical line Re(s) = 1/2, at");
        System.out.println("    heights +/-14.13: the first non-trivial pair");
        System.out.println("  - the picture is symmetric under reflection in the real axis,");
        System.out.println("    because zeta(conj(s)) = conj(zeta(s)) - which flips the hue");
        System.out.println("    but not the lightness");
        System.out.println();
        System.out.println("zeta-trivial-zeros.png");
        System.out.println("  - a row of dark points at every negative even integer, evenly");
        System.out.println("    spaced, with the pole at the right-hand end");
        System.out.println("  - nothing in this image was computable from the defining");
        System.out.println("    series; every pixel left of Re(s) = 1/2 came through the");
        System.out.println("    functional equation");
        System.out.println();
        System.out.println("zeta-first-zero.png");
        System.out.println("  - every colour meeting at a single point, the wheel wrapping");
        System.out.println("    exactly once around it");
        System.out.println("  - that single wrap is what 'simple zero' means, and it is the");
        System.out.println("    same fact the detector used when it assumed |zeta| grows");
        System.out.println("    linearly away from a zero");
        System.out.println();
        System.out.println("None of this is evidence for the Riemann Hypothesis. It is a");
        System.out.println("picture of a function, drawn from values this project computes");
        System.out.println("and validates elsewhere.");
        System.out.println();
    }

    private static void write(String fileName, String description, PlaneRegion region)
            throws IOException {

        System.out.printf("%-26s %s%n", fileName, description);
        System.out.printf("%-26s Re in [%.1f, %.1f], Im in [%.1f, %.1f], %d x %d, "
                        + "%,d evaluations, distortion %.3f%n",
                "", region.minReal(), region.maxReal(),
                region.minImaginary(), region.maxImaginary(),
                region.widthPixels(), region.heightPixels(),
                region.sampleCount(), region.aspectDistortion());

        long start = System.nanoTime();
        BufferedImage image = RENDERER.render(region);
        long renderMillis = (System.nanoTime() - start) / 1_000_000;

        Path target = OUTPUT_DIRECTORY.resolve(fileName);
        ImageIO.write(image, "png", target.toFile());

        System.out.printf("%-26s rendered in %,d ms, written to %s (%,d bytes)%n%n",
                "", renderMillis, target, Files.size(target));
    }
}