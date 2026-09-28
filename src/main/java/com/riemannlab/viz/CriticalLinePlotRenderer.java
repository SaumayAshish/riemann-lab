package com.riemannlab.viz;

import com.riemannlab.analysis.CriticalLineSample;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/**
 * Draws {@code |zeta(1/2 + it)|} as a line plot, with independently
 * established zero heights marked separately from the curve itself.
 *
 * <p>The curve comes from {@link CriticalLineSample}s - raw measurements,
 * nothing more. The markers come from a caller-supplied list of heights
 * that some other part of this project - {@code ZeroRefiner}, or a
 * published catalogue - has already proven are zeros. This renderer draws
 * both, but it does not decide which dips in the curve are zeros; it only
 * shows where the curve happens to be small next to where a zero has
 * actually been established.</p>
 */
public final class CriticalLinePlotRenderer {

    static final int MARGIN = 40;

    private static final Color BACKGROUND = Color.WHITE;
    private static final Color AXIS = Color.BLACK;
    private static final Color CURVE = new Color(0x1a, 0x5f, 0xb0);
    private static final Color ZERO_MARKER = new Color(0xd0, 0x1c, 0x1c);

    private static final float CURVE_STROKE_WIDTH = 1.6f;
    private static final float AXIS_STROKE_WIDTH = 1.0f;
    private static final float MARKER_STROKE_WIDTH = 1.2f;

    /** Creates a renderer with the default colour scheme and margins. */
    public CriticalLinePlotRenderer() {
    }

    /**
     * Renders the given samples, with the given zero heights marked.
     *
     * @param samples the curve to draw, in increasing height order; must
     *                have at least two elements
     * @param zeroHeights heights already established to be zeros; a marker
     *                    is drawn for each one that falls within the
     *                    sampled height range
     * @param widthPixels the image width in pixels
     * @param heightPixels the image height in pixels
     * @return the rendered image
     */
    public BufferedImage render(
            List<CriticalLineSample> samples, List<Double> zeroHeights,
            int widthPixels, int heightPixels) {

        Objects.requireNonNull(samples, "samples must not be null");
        Objects.requireNonNull(zeroHeights, "zeroHeights must not be null");
        if (samples.size() < 2) {
            throw new IllegalArgumentException(
                    "at least two samples are needed to draw a curve, got " + samples.size());
        }

        PlotLayout layout = layoutFor(samples, widthPixels, heightPixels);

        BufferedImage image =
                new BufferedImage(widthPixels, heightPixels, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(BACKGROUND);
            g.fillRect(0, 0, widthPixels, heightPixels);

            drawZeroMarkers(g, layout, zeroHeights);
            drawCurve(g, layout, samples);
            drawAxes(g, layout);
        } finally {
            g.dispose();
        }
        return image;
    }

    private PlotLayout layoutFor(
            List<CriticalLineSample> samples, int widthPixels, int heightPixels) {

        double minHeight = samples.get(0).height();
        double maxHeight = samples.get(samples.size() - 1).height();

        double maxMagnitude = 0.0;
        for (CriticalLineSample sample : samples) {
            maxMagnitude = Math.max(maxMagnitude, sample.magnitude());
        }
        maxMagnitude = maxMagnitude * 1.1; // headroom so the peak clears the top margin

        return new PlotLayout(
                minHeight, maxHeight, 0.0, maxMagnitude, widthPixels, heightPixels, MARGIN);
    }

    private void drawAxes(Graphics2D g, PlotLayout layout) {
        g.setColor(AXIS);
        g.setStroke(new BasicStroke(AXIS_STROKE_WIDTH));

        int baseline = (int) Math.round(layout.baselineRow());
        g.drawLine(layout.margin(), baseline, layout.widthPixels() - layout.margin(), baseline);
        g.drawLine(
                layout.margin(), layout.margin(),
                layout.margin(), layout.heightPixels() - layout.margin());
    }

    private void drawCurve(Graphics2D g, PlotLayout layout, List<CriticalLineSample> samples) {
        g.setColor(CURVE);
        g.setStroke(new BasicStroke(CURVE_STROKE_WIDTH));

        for (int i = 0; i < samples.size() - 1; i++) {
            CriticalLineSample from = samples.get(i);
            CriticalLineSample to = samples.get(i + 1);

            g.drawLine(
                    (int) Math.round(layout.xPixel(from.height())),
                    (int) Math.round(layout.yPixel(from.magnitude())),
                    (int) Math.round(layout.xPixel(to.height())),
                    (int) Math.round(layout.yPixel(to.magnitude())));
        }
    }

    private void drawZeroMarkers(Graphics2D g, PlotLayout layout, List<Double> zeroHeights) {
        g.setColor(ZERO_MARKER);
        g.setStroke(new BasicStroke(
                MARKER_STROKE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[] {6.0f, 4.0f}, 0.0f));

        int top = layout.margin();
        int bottom = layout.heightPixels() - layout.margin();

        for (double height : zeroHeights) {
            if (height < layout.minHeight() || height > layout.maxHeight()) {
                continue;
            }
            int x = (int) Math.round(layout.xPixel(height));
            g.drawLine(x, top, x, bottom);
        }
    }
}