package com.riemannlab.viz;

import com.riemannlab.core.complex.Complex;
import java.awt.Color;

/**
 * Maps a complex value to a colour: hue from its direction, brightness from
 * its size.
 *
 * <p>A complex function maps two dimensions to two dimensions, so a graph of
 * one would need four. Domain colouring escapes that by encoding the output in
 * the colour of the input point rather than in a position.</p>
 *
 * <p><strong>Why zeros are visible.</strong> Near a simple zero the function
 * behaves like {@code c(s - rho)}. Walking a small circle around the zero
 * sweeps the output through every direction exactly once, so the hue runs
 * through the entire colour wheel exactly once. A zero is therefore a point
 * where all colours meet. The pole at {@code s = 1} does the same thing with
 * the wheel running the opposite way, since {@code 1/(s-1)} reverses
 * direction.</p>
 *
 * <p>Stateless and thread-safe, which the renderer relies on.</p>
 */
public final class PhasePalette {

    private static final double TWO_PI = 2.0 * Math.PI;

    /**
     * Squashes the magnitude before mapping it to brightness. Zeta's range
     * spans hundreds of orders of magnitude; without a compressing exponent
     * the picture is black everywhere except one white pixel at the pole.
     */
    private static final double MAGNITUDE_EXPONENT = 0.25;

    /** Depth of the level-curve bands drawn at each power of two. */
    private static final double CONTOUR_DEPTH = 0.12;

    /** Painted where the function has a pole. */
    public static final int POLE_COLOUR = 0xFFFFFFFF;

    /** Painted where the evaluator refuses the point. */
    public static final int UNDEFINED_COLOUR = 0xFF303030;

    private PhasePalette() {
        throw new AssertionError("PhasePalette is a utility class and must not be instantiated");
    }

    /**
     * Colours one value.
     *
     * @param value the function value at this point; must not be null
     * @return a packed ARGB colour
     */
    public static int colourFor(Complex value) {
        double magnitude = value.magnitude();

        if (!Double.isFinite(magnitude)) {
            return POLE_COLOUR;
        }

        double lightness = lightnessFor(magnitude);

        // HSL to HSB. Brightness alone cannot reach white - a fully saturated
        // colour at maximum brightness is still a colour, not white - so the
        // saturation has to fall away as the lightness rises past the middle.
        // This is what puts black at the zeros, the pure hue where |zeta| = 1,
        // and white at the pole. Java only offers HSBtoRGB, so the conversion
        // is explicit rather than hidden in a library call.
        double brightness = lightness + Math.min(lightness, 1.0 - lightness);
        double saturation = brightness <= 0.0
                ? 0.0
                : 2.0 * (1.0 - lightness / brightness);

        return Color.HSBtoRGB(
                (float) hueFor(value), (float) saturation, (float) brightness);
    }

    /**
     * The argument, rescaled from {@code (-pi, pi]} onto {@code [0, 1)}. The
     * two are both circles, which is why this mapping is the natural one
     * rather than an arbitrary choice of colours.
     */
    static double hueFor(Complex value) {
        double argument = Math.atan2(value.imaginary(), value.real());

        return ((argument / TWO_PI) + 1.0) % 1.0;
    }

    /**
     * Lightness from magnitude: zero is black, one is the pure hue, infinity is
     * white, with faint level curves at each power of two so the magnitude can
     * be read off the picture rather than only compared.
     */
    static double lightnessFor(double magnitude) {
        if (!(magnitude > 0.0)) {
            return 0.0;
        }

        double ramp = Math.pow(magnitude, MAGNITUDE_EXPONENT);
        double base = ramp / (1.0 + ramp);

        double octave = Math.log(magnitude) / Math.log(2.0);
        double withinOctave = octave - Math.floor(octave);
        double shading = 1.0 - CONTOUR_DEPTH * (1.0 - withinOctave);

        return Math.clamp(base * shading, 0.0, 1.0);
    }
}