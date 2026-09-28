package com.riemannlab.viz;

import com.riemannlab.core.complex.Complex;
import com.riemannlab.zeta.ZetaEvaluator;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a region of the complex plane as a domain-coloured image.
 *
 * <p>One zeta evaluation per pixel, which is the entire cost: a modest
 * 800x1200 image is nearly a million evaluations, each running a few dozen
 * complex powers.</p>
 *
 * <p><strong>This is where the evaluators' immutability finally pays.</strong>
 * Every evaluator in this project was written stateless and documented
 * thread-safe, at some cost in convenience. That decision is what allows the
 * row loop below to be parallel without a lock, a copy, or a single change to
 * anything it calls. Rows write to disjoint pixels, and the terminal operation
 * of the stream establishes the happens-before that makes those writes visible
 * afterwards.</p>
 *
 * <p>Points the evaluator refuses - the pole, or arguments outside its
 * domain - are painted rather than propagated. A picture with a white dot at
 * {@code s = 1} is more useful than a stack trace, and the distinct colour for
 * a refused point means the limits of the evaluator are visible in the output
 * rather than hidden.</p>
 */
public final class DomainColourRenderer {

    private static final Logger log = LoggerFactory.getLogger(DomainColourRenderer.class);

    private final ZetaEvaluator evaluator;

    /**
     * Creates a renderer backed by the given evaluator.
     *
     * @param evaluator the function to draw; must not be null and must be
     *                  thread-safe
     */
    public DomainColourRenderer(ZetaEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    }

    /**
     * Renders the region.
     *
     * @param region what to draw and at what resolution; must not be null
     * @return the image, never null
     */
    public BufferedImage render(PlaneRegion region) {
        Objects.requireNonNull(region, "region must not be null");

        BufferedImage image = new BufferedImage(
                region.widthPixels(), region.heightPixels(), BufferedImage.TYPE_INT_RGB);

        long startNanos = System.nanoTime();

        IntStream.range(0, region.heightPixels()).parallel().forEach(y -> {
            for (int x = 0; x < region.widthPixels(); x++) {
                image.setRGB(x, y, colourAt(region.pointAt(x, y)));
            }
        });

        log.info("Rendered {}x{} ({} evaluations) with {} in {} ms",
                region.widthPixels(), region.heightPixels(), region.sampleCount(),
                evaluator.name(), (System.nanoTime() - startNanos) / 1_000_000);

        return image;
    }

    private int colourAt(Complex s) {
        try {
            return PhasePalette.colourFor(evaluator.evaluate(s).value());
        } catch (ArithmeticException pole) {
            return PhasePalette.POLE_COLOUR;
        } catch (IllegalArgumentException outsideDomain) {
            return PhasePalette.UNDEFINED_COLOUR;
        }
    }
}