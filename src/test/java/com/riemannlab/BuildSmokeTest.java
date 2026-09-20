package com.riemannlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that the build toolchain is correctly wired, and pins the two
 * floating-point facts that every later numerical decision in RiemannLab rests on.
 *
 * <p>These tests contain no project logic. If they fail, the problem is the
 * environment, not the mathematics.</p>
 */
class BuildSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(BuildSmokeTest.class);

    /**
     * Machine epsilon for an IEEE-754 {@code double}: the distance from 1.0 to the
     * next representable value above it. Equal to 2^-52.
     */
    private static final double MACHINE_EPSILON = 2.220446049250313E-16;

    @Test
    @DisplayName("Maven, JUnit 5 and SLF4J are correctly wired together")
    void toolchainIsAlive() {
        log.info("RiemannLab smoke test running on Java {}", System.getProperty("java.version"));
        assertTrue(true, "Reaching this line at all proves the test runner executed.");
    }

    @Test
    @DisplayName("This JVM provides IEEE-754 doubles with about 15.95 decimal digits")
    void doublePrecisionIsAsExpected() {
        assertEquals(MACHINE_EPSILON, Math.ulp(1.0),
                "Every precision decision in RiemannLab assumes this exact value.");
    }

    @Test
    @DisplayName("Floating-point addition is inexact, which is why we will never compare with ==")
    void floatingPointAdditionIsNotExact() {
        double sum = 0.1 + 0.2;

        assertNotEquals(0.3, sum,
                "0.1 + 0.2 is not exactly 0.3 in binary floating point.");

        assertEquals(0.3, sum, 1e-15,
                "But it lies within 1e-15 of 0.3 - tolerance comparison is the only honest test.");

        log.debug("0.1 + 0.2 evaluates to {}", sum);
    }
}