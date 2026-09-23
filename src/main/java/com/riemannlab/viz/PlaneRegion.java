package com.riemannlab.viz;

import com.riemannlab.core.complex.Complex;

/**
 * A rectangle of the complex plane together with the pixel grid it is sampled
 * on, and the mapping between the two.
 *
 * <p>Separating this from the rendering is what makes the geometry testable.
 * Everything that can go subtly wrong in a plot - an off-by-one at the edges,
 * sampling corners instead of centres, a flipped axis, non-square pixels that
 * silently distort the picture - lives here, in a record with no dependency on
 * {@code java.awt} at all.</p>
 *
 * <p><strong>The vertical flip.</strong> Pixel row 0 is the top of the image,
 * and the top of the image is the <em>largest</em> imaginary part. Screen
 * coordinates run downward while the imaginary axis runs upward, so the
 * vertical mapping subtracts where the horizontal one adds.</p>
 *
 * @param minReal        left edge of the rectangle
 * @param maxReal        right edge
 * @param minImaginary   bottom edge
 * @param maxImaginary   top edge
 * @param widthPixels    horizontal sample count
 * @param heightPixels   vertical sample count
 */
public record PlaneRegion(
        double minReal, double maxReal,
        double minImaginary, double maxImaginary,
        int widthPixels, int heightPixels) {

    public PlaneRegion {
        if (!(maxReal > minReal)) {
            throw new IllegalArgumentException(
                    "maxReal must exceed minReal, was " + minReal + " to " + maxReal);
        }
        if (!(maxImaginary > minImaginary)) {
            throw new IllegalArgumentException(
                    "maxImaginary must exceed minImaginary, was "
                            + minImaginary + " to " + maxImaginary);
        }
        if (widthPixels < 1) {
            throw new IllegalArgumentException("widthPixels must be at least 1, was " + widthPixels);
        }
        if (heightPixels < 1) {
            throw new IllegalArgumentException(
                    "heightPixels must be at least 1, was " + heightPixels);
        }
    }

    public double realSpan() {
        return maxReal - minReal;
    }

    public double imaginarySpan() {
        return maxImaginary - minImaginary;
    }

    public double realPerPixel() {
        return realSpan() / widthPixels;
    }

    public double imaginaryPerPixel() {
        return imaginarySpan() / heightPixels;
    }

    /**
     * The complex number at the <strong>centre</strong> of a pixel.
     *
     * @param x column, 0 at the left
     * @param y row, 0 at the <em>top</em>
     * @return the point that pixel samples
     * @throws IndexOutOfBoundsException if the pixel is outside the grid
     */
    public Complex pointAt(int x, int y) {
        if (x < 0 || x >= widthPixels) {
            throw new IndexOutOfBoundsException(
                    "x must be in [0, " + widthPixels + "), was " + x);
        }
        if (y < 0 || y >= heightPixels) {
            throw new IndexOutOfBoundsException(
                    "y must be in [0, " + heightPixels + "), was " + y);
        }

        return Complex.of(
                minReal + (x + 0.5) * realPerPixel(),
                maxImaginary - (y + 0.5) * imaginaryPerPixel());
    }

    /**
     * The ratio of horizontal to vertical scale. One means square pixels and an
     * undistorted picture; anything else means the plane has been stretched,
     * which is sometimes wanted and should always be deliberate.
     *
     * @return {@code realPerPixel / imaginaryPerPixel}
     */
    public double aspectDistortion() {
        return realPerPixel() / imaginaryPerPixel();
    }

    /** Total sample count, which is also the number of zeta evaluations. */
    public long sampleCount() {
        return (long) widthPixels * heightPixels;
    }
}