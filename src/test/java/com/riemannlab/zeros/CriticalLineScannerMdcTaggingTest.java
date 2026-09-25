package com.riemannlab.zeros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.riemannlab.zeta.AcceleratedEtaEvaluator;
import com.riemannlab.zeta.ContinuedZetaEvaluator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Proves that {@link CriticalLineScanner#scan} and
 * {@link CriticalLineScanner#scanParallel} tag every log line they write
 * with which one produced it, via MDC's {@code scanMode} key.
 *
 * <p>{@code logback-test.xml} raises {@code com.riemannlab} to WARN during
 * test runs, so the DEBUG/INFO lines under test never reach any appender by
 * default - Logback filters by the logger's effective level before an event
 * is offered to appenders at all. This test lowers the level on the specific
 * logger under test for its own duration, captures events with an in-memory
 * {@link ListAppender} instead of printing them, and restores the original
 * level afterward so nothing leaks into other tests sharing this JVM.</p>
 */
class CriticalLineScannerMdcTaggingTest {

    private static final double SCAN_START = 1.0;
    private static final double SCAN_END = 55.0;
    private static final double SCAN_STEP = 0.1;

    private Logger scannerLogger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void captureLogEvents() {
        scannerLogger = (Logger) LoggerFactory.getLogger(CriticalLineScanner.class);
        originalLevel = scannerLogger.getLevel();
        scannerLogger.setLevel(Level.DEBUG);

        appender = new ListAppender<>();
        appender.start();
        scannerLogger.addAppender(appender);
    }

    @AfterEach
    void restoreLogging() {
        scannerLogger.detachAppender(appender);
        scannerLogger.setLevel(originalLevel);
    }

    @Test
    void sequentialScanTagsEveryLogLineAsSequential() {
        CriticalLineScanner scanner =
                new CriticalLineScanner(new ContinuedZetaEvaluator(new AcceleratedEtaEvaluator()));

        scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);

        List<ILoggingEvent> events = appender.list;
        assertTrue(events.size() > 1,
                "expected scan() to log the summary line plus at least one minimum");

        for (ILoggingEvent event : events) {
            assertEquals("sequential", event.getMDCPropertyMap().get("scanMode"),
                    () -> "unexpected scanMode for: " + event.getFormattedMessage());
        }
    }

    @Test
    void parallelScanTagsEveryLogLineAsParallel() {
        CriticalLineScanner scanner =
                new CriticalLineScanner(new ContinuedZetaEvaluator(new AcceleratedEtaEvaluator()));

        scanner.scanParallel(SCAN_START, SCAN_END, SCAN_STEP);

        List<ILoggingEvent> events = appender.list;
        assertTrue(events.size() > 1,
                "expected scanParallel() to log the summary line plus at least one minimum");

        for (ILoggingEvent event : events) {
            assertEquals("parallel", event.getMDCPropertyMap().get("scanMode"),
                    () -> "unexpected scanMode for: " + event.getFormattedMessage());
        }
    }

    @Test
    void sequentialAndParallelTagsDoNotBleedIntoEachOther() {
        CriticalLineScanner scanner =
                new CriticalLineScanner(new ContinuedZetaEvaluator(new AcceleratedEtaEvaluator()));

        scanner.scan(SCAN_START, SCAN_END, SCAN_STEP);
        scanner.scanParallel(SCAN_START, SCAN_END, SCAN_STEP);

        List<ILoggingEvent> events = appender.list;

        long sequentialCount = events.stream()
                .filter(e -> "sequential".equals(e.getMDCPropertyMap().get("scanMode")))
                .count();
        long parallelCount = events.stream()
                .filter(e -> "parallel".equals(e.getMDCPropertyMap().get("scanMode")))
                .count();

        assertTrue(sequentialCount > 0 && parallelCount > 0,
                "expected both tags to appear: sequential=" + sequentialCount
                        + " parallel=" + parallelCount);
        assertEquals(events.size(), sequentialCount + parallelCount,
                "every captured line should carry exactly one of the two tags");
    }
}