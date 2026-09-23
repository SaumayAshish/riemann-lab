package com.riemannlab.viz;

/**
 * Maps data coordinates - a height {@code t} and a magnitude - onto pixel
 * coordinates for a two-dimensional line plot.
 *
 * <p>The same vertical flip as {@link PlaneRegion} applies here: magnitude
 * increases upward on the page, but pixel rows increase downward, so a
 * larger magnitude must map to a smaller {@code y}.</p>
 *
 * <p>A margin is reserved on every side for the axes, so the plotted curve
 * never touches the edge of the image.</p>
 */
public record PlotLayout(
        double minHeight,
        double maxHeight,
        double minMagnitude,
        double maxMagnitude,
        int widthPixels,
        int heightPixels,
        int margin) {

    public PlotLayout {
        if (!(maxHeight > minHeight)) {
            throw new IllegalArgumentException(
                    "maxHeight must exceed minHeight, got [" + minHeight + ", " + maxHeight + "]");
        }
        if (!(maxMagnitude > minMagnitude)) {
            throw new IllegalArgumentException(
                    "maxMagnitude must exceed minMagnitude, got ["
                            + minMagnitude + ", " + maxMagnitude + "]");
        }
        if (margin < 0) {
            throw new IllegalArgumentException("margin must not be negative, got " + margin);
        }
        if (widthPixels <= 2 * margin || heightPixels <= 2 * margin) {
            throw new IllegalArgumentException(
                    "the plot area must be larger than the margins: " + widthPixels
                            + "x" + heightPixels + " with margin " + margin);
        }
    }

    /** The plotting area's width in pixels, after both side margins. */
    public int plotWidth() {
        return widthPixels - 2 * margin;
    }

    /** The plotting area's height in pixels, after both top/bottom margins. */
    public int plotHeight() {
        return heightPixels - 2 * margin;
    }

    /** The pixel column a given height maps to. */
    public double xPixel(double height) {
        double fraction = (height - minHeight) / (maxHeight - minHeight);
        return margin + fraction * plotWidth();
    }

    /** The pixel row a given magnitude maps to - larger magnitude, smaller row. */
    public double yPixel(double magnitude) {
        double fraction = (magnitude - minMagnitude) / (maxMagnitude - minMagnitude);
        return margin + (1.0 - fraction) * plotHeight();
    }

    /** The row the x-axis (magnitude = minMagnitude) is drawn on. */
    public double baselineRow() {
        return yPixel(minMagnitude);
    }
}