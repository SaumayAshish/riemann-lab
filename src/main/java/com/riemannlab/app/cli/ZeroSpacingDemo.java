package com.riemannlab.app.cli;

import com.riemannlab.analysis.ZeroSpacing;
import com.riemannlab.analysis.ZeroSpacingTable;
import com.riemannlab.validation.KnownZeroCatalog;
import com.riemannlab.zeros.CriticalLineScanner;
import com.riemannlab.zeros.RefinedZero;
import com.riemannlab.zeros.ZeroCandidate;
import com.riemannlab.zeros.ZeroRefiner;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Builds a table of zero heights and the raw spacing between them, from a
 * live scan-and-refine run - the same pipeline {@code ZeroFinderDemo}
 * validated - and cross-checks it against {@link KnownZeroCatalog}.
 *
 * <p>This closes the "zero analysis" half of the project's roadmap. The
 * spacing here is deliberately raw, not normalized - see {@link
 * ZeroSpacingTable}'s own documentation for why.</p>
 */
public final class ZeroSpacingDemo {

    private static final Path OUTPUT_DIRECTORY = Path.of("data");
    private static final String OUTPUT_FILE = "zero-spacing.csv";

    private static final double SCAN_START = 1.0;
    private static final double SCAN_END = 55.0;
    private static final double SCAN_STEP = 0.1;

    private static final AcceleratedEtaEvaluator EVALUATOR = new AcceleratedEtaEvaluator();
    private static final CriticalLineScanner SCANNER = new CriticalLineScanner(EVALUATOR);
    private static final ZeroRefiner REFINER = new ZeroRefiner(EVALUATOR);

    private ZeroSpacingDemo() {
        throw new AssertionError("ZeroSpacingDemo is an entry point and must not be instantiated");
    }
    /**
     * Demonstrates computing spacing statistics between zeros.
     *
     * @param args unused
     * @throws IOException if the output cannot be written
     */
    public static void main(String[] args) throws IOException {
        printHeader();

        List<ZeroCandidate> candidates = SCANNER.scanForZeros(SCAN_START, SCAN_END, SCAN_STEP);
        List<RefinedZero> refined = REFINER.refineAll(candidates);
        List<Double> heights = refined.stream()
                .filter(RefinedZero::isConfirmed)
                .map(RefinedZero::height)
                .toList();

        List<ZeroSpacing> table = ZeroSpacingTable.compute(heights);

        printTable(table);
        writeCsv(table);
        printDisclaimer();
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("RiemannLab - zero heights and raw spacing");
        System.out.println("=".repeat(92));
        System.out.printf("scan: t in [%.1f, %.1f], step %.2f, then refine and keep confirmed zeros%n",
                SCAN_START, SCAN_END, SCAN_STEP);
        System.out.println();
        System.out.println("Spacing here is raw, not normalized - see ZeroSpacingTable's JavaDoc");
        System.out.println("for why comparing these gaps across widely different heights would");
        System.out.println("be misleading.");
    }

    private static void printTable(List<ZeroSpacing> table) {
        section("Heights and spacing, against the published catalogue");
        System.out.printf("%3s  %22s  %16s  %24s  %12s%n",
                "n", "gamma_n", "delta gamma_n", "published gamma_n", "difference");
        System.out.println("-".repeat(92));

        for (ZeroSpacing entry : table) {
            OptionalDouble published = KnownZeroCatalog.heightOf(entry.index());
            String spacingStr = entry.isFirst()
                    ? "-" : String.format("%16.10f", entry.spacingFromPrevious());
            String publishedStr = published.isPresent()
                    ? String.format("%24.15f", published.getAsDouble()) : "-";
            String differenceStr = published.isPresent()
                    ? String.format("%12.2e", Math.abs(entry.height() - published.getAsDouble()))
                    : "-";

            System.out.printf("%3d  %22.15f  %16s  %24s  %12s%n",
                    entry.index(), entry.height(), spacingStr, publishedStr, differenceStr);
        }

        System.out.println();
        System.out.printf("%d zeros in this table, %d checked against the catalogue.%n",
                table.size(), Math.min(table.size(), KnownZeroCatalog.count()));
    }

    private static void writeCsv(List<ZeroSpacing> table) throws IOException {
        Files.createDirectories(OUTPUT_DIRECTORY);
        Path target = OUTPUT_DIRECTORY.resolve(OUTPUT_FILE);

        StringBuilder csv = new StringBuilder();
        csv.append("n,gamma_n,delta_gamma_n,published_gamma_n,difference\n");

        for (ZeroSpacing entry : table) {
            OptionalDouble published = KnownZeroCatalog.heightOf(entry.index());
            String spacingCell = entry.isFirst() ? "" : Double.toString(entry.spacingFromPrevious());
            String publishedCell = published.isPresent()
                    ? Double.toString(published.getAsDouble()) : "";
            String differenceCell = published.isPresent()
                    ? Double.toString(Math.abs(entry.height() - published.getAsDouble())) : "";

            csv.append(entry.index()).append(',')
                    .append(entry.height()).append(',')
                    .append(spacingCell).append(',')
                    .append(publishedCell).append(',')
                    .append(differenceCell)
                    .append('\n');
        }

        Files.writeString(target, csv.toString());

        section("CSV written");
        System.out.printf("%s  (%d rows, %,d bytes)%n", target, table.size(), Files.size(target));
    }

    private static void printDisclaimer() {
        section("What this does and does not establish");
        System.out.println("Established, for the range scanned:");
        System.out.println("  - a table of zero heights and their raw spacing, built entirely");
        System.out.println("    from this project's own scan-and-refine pipeline");
        System.out.println("  - every height in this range matches the published catalogue to");
        System.out.println("    within the precision already demonstrated in ZeroFinderDemo");
        System.out.println();
        System.out.println("NOT established:");
        System.out.println("  - a normalized spacing distribution - that needs the zero-counting");
        System.out.println("    function, which this project has not built yet");
        System.out.println("  - anything about zeros outside the range scanned");
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
        System.out.println("=".repeat(92));
    }
}