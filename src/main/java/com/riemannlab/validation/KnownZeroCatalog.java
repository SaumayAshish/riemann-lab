package com.riemannlab.validation;

import java.util.OptionalDouble;

/**
 * Published reference heights for the first several non-trivial zeros of
 * zeta, used only to check what this project computes independently -
 * never as an input to any scan or refinement.
 */
public final class KnownZeroCatalog {

    private static final double[] FIRST_ELEVEN_HEIGHTS = {
            14.134725141734693,
            21.022039638771555,
            25.010857580145688,
            30.424876125859513,
            32.935061587739189,
            37.586178158825671,
            40.918719012147495,
            43.327073280914999,
            48.005150881167159,
            49.773832477672302,
            52.970321477714460
    };

    private KnownZeroCatalog() {
        throw new AssertionError(
                "KnownZeroCatalog is a constant source and must not be instantiated");
    }

    /** The published heights this catalogue knows, in ascending order. */
    public static double[] firstElevenHeights() {
        return FIRST_ELEVEN_HEIGHTS.clone(); // defensive copy - arrays are mutable
    }

    /** The reference height for the nth zero (1-indexed), or empty if not catalogued. */
    public static OptionalDouble heightOf(int n) {
        if (n < 1 || n > FIRST_ELEVEN_HEIGHTS.length) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(FIRST_ELEVEN_HEIGHTS[n - 1]);
    }

    /** How many heights this catalogue knows. */
    public static int count() {
        return FIRST_ELEVEN_HEIGHTS.length;
    }
}