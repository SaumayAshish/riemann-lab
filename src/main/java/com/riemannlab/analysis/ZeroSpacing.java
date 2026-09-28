package com.riemannlab.analysis;

/**
 * One entry in a table of zero heights and their spacing from the previous
 * zero: gamma_n and delta-gamma_n = gamma_n - gamma_(n-1).
 *
 * <p>{@code index} is 1-based, matching how heights are usually listed in
 * the literature. The first entry in any table has no previous zero to
 * subtract, so its spacing is {@code Double.NaN} rather than zero - zero
 * would claim two zeros sit on top of each other, which has never
 * happened, and NaN says "not applicable" instead of "measured as
 * zero."</p>
 *
 * @param index the 1-based position of this zero in the table
 * @param height the imaginary part gamma_n of this zero
 * @param spacingFromPrevious gamma_n minus gamma_(n-1), or {@code NaN} for the first entry
 */
public record ZeroSpacing(int index, double height, double spacingFromPrevious) {

    /** Validates that index is at least 1, height is finite, and spacingFromPrevious is either NaN or a positive finite number. */
    public ZeroSpacing {
        if (index < 1) {
            throw new IllegalArgumentException("index must be at least 1, got " + index);
        }
        if (!Double.isFinite(height)) {
            throw new IllegalArgumentException("height must be finite, got " + height);
        }
        boolean spacingIsValid = Double.isNaN(spacingFromPrevious)
                || (Double.isFinite(spacingFromPrevious) && spacingFromPrevious > 0.0);
        if (!spacingIsValid) {
            throw new IllegalArgumentException(
                    "spacingFromPrevious must be NaN or a positive finite number, got "
                            + spacingFromPrevious);
        }
    }

    /**
     * Whether this is the first entry in its table, with no previous zero.
     *
     * @return {@code true} if this entry has no previous zero to compare against
     */
    public boolean isFirst() {
        return Double.isNaN(spacingFromPrevious);
    }
}