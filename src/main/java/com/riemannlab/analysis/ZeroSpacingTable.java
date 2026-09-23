package com.riemannlab.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a table of zero heights and the raw spacing between consecutive
 * ones: gamma_n and delta-gamma_n = gamma_n - gamma_(n-1).
 *
 * <p>This is deliberately the un-normalized spacing. Zeros sit closer
 * together, on average, as height grows, so comparing these gaps directly
 * across different height ranges is misleading. Turning this into a
 * distribution comparable across heights needs dividing by the local zero
 * density, which needs the zero-counting function - not built yet. This
 * class stops at the raw numbers, on purpose.</p>
 */
public final class ZeroSpacingTable {

    private ZeroSpacingTable() {
        throw new AssertionError(
                "ZeroSpacingTable is a utility class and must not be instantiated");
    }

    /**
     * Builds the spacing table for the given heights, which must already be
     * sorted in strictly ascending order - the order zeros naturally come
     * out of a scan run from low to high.
     */
    public static List<ZeroSpacing> compute(List<Double> heights) {
        Objects.requireNonNull(heights, "heights must not be null");
        if (heights.isEmpty()) {
            throw new IllegalArgumentException("heights must not be empty");
        }

        List<ZeroSpacing> table = new ArrayList<>(heights.size());
        double previous = Double.NaN;

        for (int i = 0; i < heights.size(); i++) {
            double height = heights.get(i);
            if (i > 0 && !(height > previous)) {
                throw new IllegalArgumentException(
                        "heights must be strictly ascending: entry " + i + " (" + height
                                + ") does not exceed entry " + (i - 1) + " (" + previous + ")");
            }

            double spacing = (i == 0) ? Double.NaN : height - previous;
            table.add(new ZeroSpacing(i + 1, height, spacing));
            previous = height;
        }

        return table;
    }
}