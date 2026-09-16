/**
 * AnesthesiaCalculator unit tests
 * Run in browser DevTools console or Node.js (jsdom) environment.
 * 
 * Usage: Import the AnesthesiaCalculator class from index.html and run tests here.
 */

// ============================================================
// Test runner (minimal, no dependencies)
// ============================================================
let TESTS_RUN = 0;
let TESTS_PASSED = 0;
let TESTS_FAILED = 0;

function assert(condition, message) {
    TESTS_RUN++;
    if (condition) {
        TESTS_PASSED++;
    } else {
        TESTS_FAILED++;
        console.error(`FAIL: ${message}`);
    }
}

function assertEquals(actual, expected, message) {
    TESTS_RUN++;
    if (actual === expected) {
        TESTS_PASSED++;
    } else {
        TESTS_FAILED++;
        console.error(`FAIL: ${message} (expected: ${expected}, actual: ${actual})`);
    }
}

function assertDeepEqual(actual, expected, message) {
    TESTS_RUN++;
    const a = JSON.stringify(actual);
    const e = JSON.stringify(expected);
    if (a === e) {
        TESTS_PASSED++;
    } else {
        TESTS_FAILED++;
        console.error(`FAIL: ${message}`);
        console.error(`  expected: ${e}`);
        console.error(`  actual:   ${a}`);
    }
}

function runTests(name, testFn) {
    console.log(`\n--- ${name} ---`);
    testFn();
}

function printResults() {
    console.log(`\n=============================`);
    console.log(`Tests: ${TESTS_PASSED}/${TESTS_RUN} passed`);
    if (TESTS_FAILED > 0) {
        console.log(`FAILURES: ${TESTS_FAILED}`);
    } else {
        console.log(`ALL PASSED`);
    }
    console.log(`=============================`);
}

// ============================================================
// Tests (use AnesthesiaCalculator from index.html)
// ============================================================

function runAllTests() {
    // Access the calculator instance from the page context
    // In a browser: window.calculator
    // In jsdom: globalThis.calculator
    
    const calc = typeof window !== 'undefined' ? window.calculator : globalThis.calculator;
    
    if (!calc) {
        console.error('ERROR: AnesthesiaCalculator not found. Make sure index.html is loaded first.');
        return;
    }

    // ---- Test: Basic calculation (single level, normal severity) ----
    runTests('Basic Calculation - Level 1 Normal', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        assertEquals(result.basePoints, 18200, 'Level 1 normal base points');
        assertEquals(result.timeExtensionPoints, 0, 'No time extension for 60 min');
        assertEquals(result.totalPoints, 18200, 'Total = base for simple case');
        assertEquals(result.highestLevel, 1, 'Highest level is 1');
        assertEquals(result.totalMinutes, 60, 'Total minutes is 60');
    });

    // ---- Test: Basic calculation - Level 1 Difficult ----
    runTests('Basic Calculation - Level 1 Difficult', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'difficult';
        
        const result = calc.calculate();
        
        assertEquals(result.basePoints, 24900, 'Level 1 difficult base points');
        assertEquals(result.timeExtensionPoints, 0, 'No time extension for 60 min');
        assertEquals(result.totalPoints, 24900, 'Total = base for simple case');
    });

    // ---- Test: Time extension - exactly 120 minutes ----
    runTests('Time Extension - Exactly 120 min', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [120, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        assertEquals(result.basePoints, 18200, 'Level 1 normal base');
        assertEquals(result.timeExtensionPoints, 0, 'No extension at exactly 120 min');
    });

    // ---- Test: Time extension - 121 minutes (first extension block) ----
    runTests('Time Extension - 121 min (first block)', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [121, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        // 120 min base, 1 min overtime -> ceil(1/30) = 1 block
        // Level 1: 1800 points per block
        assertEquals(result.timeExtensionPoints, 1800, 'One extension block at 121 min');
    });

    // ---- Test: Time extension - 150 minutes (2 blocks) ----
    runTests('Time Extension - 150 min', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [150, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        // 120 min base, 30 min overtime -> ceil(30/30) = 1 block
        assertEquals(result.timeExtensionPoints, 1800, 'One extension block at 150 min');
    });

    // ---- Test: Time extension - 151 minutes (2 blocks) ----
    runTests('Time Extension - 151 min', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [151, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        // 120 min base, 31 min overtime -> ceil(31/30) = 2 blocks
        assertEquals(result.timeExtensionPoints, 3600, 'Two extension blocks at 151 min');
    });

    // ---- Test: Multi-level calculation ----
    runTests('Multi-Level Calculation', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 90, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        // Level 1: 60 min, Level 2: 90 min
        // Highest level = 1
        assertEquals(result.highestLevel, 1, 'Highest level is 1');
        assertEquals(result.basePoints, 18200, 'Level 1 normal base');
        assertEquals(result.totalMinutes, 150, 'Total minutes = 60 + 90');
    });

    // ---- Test: Empty calculation (0 minutes) ----
    runTests('Empty Calculation - 0 Minutes', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [0, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        assertEquals(result.totalPoints, 0, 'Total points is 0');
        assertEquals(result.basePoints, 0, 'Base points is 0');
        assertEquals(result.timeExtensionPoints, 0, 'Time extension is 0');
        assertEquals(result.totalMinutes, 0, 'Total minutes is 0');
    });

    // ---- Test: Epidural anesthesia addition ----
    runTests('Epidural Anesthesia - Lumbar', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasEpiduralAnesthesia = true;
        calc.epiduralLocation = 'lumbar';
        
        const result = calc.calculate();
        
        assertEquals(result.epiduralPoints, 400, 'Lumbar epidural base points');
        assertEquals(result.epiduralTimeExtensionPoints, 0, 'No epidural extension for <= 120 min');
    });

    // ---- Test: Epidural anesthesia - Cervical ----
    runTests('Epidural Anesthesia - Cervical', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasEpiduralAnesthesia = true;
        calc.epiduralLocation = 'cervical';
        
        const result = calc.calculate();
        
        assertEquals(result.epiduralPoints, 750, 'Cervical epidural base points');
    });

    // ---- Test: Epidural anesthesia - Sacral ----
    runTests('Epidural Anesthesia - Sacral', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasEpiduralAnesthesia = true;
        calc.epiduralLocation = 'sacral';
        
        const result = calc.calculate();
        
        assertEquals(result.epiduralPoints, 170, 'Sacral epidural base points');
    });

    // ---- Test: Epidural time extension (> 120 min) ----
    runTests('Epidural Time Extension - > 120 min', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [180, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasEpiduralAnesthesia = true;
        calc.epiduralLocation = 'lumbar';
        
        const result = calc.calculate();
        
        // 180 - 120 = 60 min overtime -> ceil(60/30) = 2 blocks
        // Lumbar: 200 per block -> 400
        assertEquals(result.epiduralTimeExtensionPoints, 400, '2 epidural extension blocks');
    });

    // ---- Test: Nerve block - Other ----
    runTests('Nerve Block - Other', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasNerveBlock = true;
        calc.nerveBlockType = 'other';
        
        const result = calc.calculate();
        
        assertEquals(result.nerveBlockPoints, 45, 'Other nerve block points');
    });

    // ---- Test: Nerve block - Epidural substitute ----
    runTests('Nerve Block - Epidural Substitute', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasNerveBlock = true;
        calc.nerveBlockType = 'epiduralSubstitute';
        
        const result = calc.calculate();
        
        assertEquals(result.nerveBlockPoints, 450, 'Epidural substitute nerve block points');
    });

    // ---- Test: Nerve block epiduralSubstitute disables epidural ----
    runTests('Nerve Block - Epidural Substitue Disables Epidural', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasEpiduralAnesthesia = true;
        calc.epiduralLocation = 'lumbar';
        calc.hasNerveBlock = true;
        calc.nerveBlockType = 'epiduralSubstitute';
        
        const result = calc.calculate();
        
        assertEquals(result.epiduralPoints, 0, 'Epidural points disabled when nerve block = epiduralSubstitute');
        assertEquals(result.epiduralTimeExtensionPoints, 0, 'Epidural extension disabled when nerve block = epiduralSubstitute');
        assertEquals(result.nerveBlockPoints, 450, 'Nerve block points still applied');
    });

    // ---- Test: Overtime addition (1.4x) ----
    runTests('Overtime Addition - 1.4x', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasOvertimeAddition = true;
        
        const result = calc.calculate();
        
        // (18200 * 1.4) floor = 25480
        const expected = Math.floor(18200 * 1.4);
        assertEquals(result.totalPoints, expected, 'Overtime 1.4x applied');
    });

    // ---- Test: Night/holiday addition (1.8x) ----
    runTests('Night/Holiday Addition - 1.8x', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasNightHolidayAddition = true;
        
        const result = calc.calculate();
        
        // (18200 * 1.8) floor = 32760
        const expected = Math.floor(18200 * 1.8);
        assertEquals(result.totalPoints, expected, 'Night/holiday 1.8x applied');
    });

    // ---- Test: Both overtime and night/holiday -> 1.8x wins ----
    runTests('Both Overtime + Night/Holiday - 1.8x Wins', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasOvertimeAddition = true;
        calc.hasNightHolidayAddition = true;
        
        const result = calc.calculate();
        
        // 1.8x should win (higher multiplier)
        const expected = Math.floor(18200 * 1.8);
        assertEquals(result.totalPoints, expected, 'Night/holiday 1.8x wins over overtime 1.4x');
    });

    // ---- Test: Nerve block NOT multiplied (v1.3 fix) ----
    runTests('Nerve Block Not Multiplied (v1.3 Fix)', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasNerveBlock = true;
        calc.nerveBlockType = 'other';
        calc.hasOvertimeAddition = true;
        
        const result = calc.calculate();
        
        // base * 1.4 + nerveBlock (not multiplied)
        const expected = Math.floor(18200 * 1.4) + 45;
        assertEquals(result.totalPoints, expected, 'Nerve block points NOT multiplied (v1.3 fix)');
    });

    // ---- Test: Epidural + Overtime ----
    runTests('Epidural + Overtime', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasEpiduralAnesthesia = true;
        calc.epiduralLocation = 'lumbar';
        calc.hasOvertimeAddition = true;
        
        const result = calc.calculate();
        
        // (18200 + 400) * 1.4 = 18600 * 1.4 = 26040
        const expected = Math.floor((18200 + 400) * 1.4);
        assertEquals(result.totalPoints, expected, 'Epidural + overtime calculation');
    });

    // ---- Test: Multi-level time extension borrowing ----
    runTests('Multi-Level Time Extension Borrowing', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [30, 150, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        // Level 1: 30 min, Level 2: 150 min
        // Highest level = 1 (level 1)
        // Level 1: 30 min total, 30 min base (all used), 0 extension
        // Level 2: 150 min total, 90 min base (remaining from 120), 60 min extension
        // Level 2: ceil(60/30) = 2 blocks * 1200 = 2400
        assertEquals(result.timeExtensionPoints > 0, true, 'Time extension should exist for multi-level');
    });

    // ---- Test: getActiveLevels ----
    runTests('Get Active Levels', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [0, 90, 0, 60, 0];
        
        const active = calc.getActiveLevels();
        
        assertEquals(active.length, 2, 'Two active levels');
        assertEquals(active[0].level, 2, 'First active level is 2');
        assertEquals(active[1].level, 4, 'Second active level is 4');
    });

    // ---- Test: getTotalMinutes ----
    runTests('Get Total Minutes', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [30, 60, 90, 0, 0];
        
        const total = calc.getTotalMinutes();
        
        assertEquals(total, 180, 'Total minutes = 30 + 60 + 90');
    });

    // ---- Test: calculateTimeExtension method directly ----
    runTests('Calculate Time Extension Directly', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [200, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const ext = calc.calculateTimeExtension();
        
        // 200 - 120 = 80 min overtime -> ceil(80/30) = 3 blocks
        // Level 1: 1800 per block -> 5400
        assertEquals(ext.total, 5400, 'Time extension total for 200 min');
        assert(ext.details.length >= 1, 'At least one detail entry');
    });

    // ---- Test: Five level empty ----
    runTests('Five Level All Empty', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [0, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        assertEquals(result.totalPoints, 0, 'Zero points when no anesthesia time');
        assertEquals(result.activeLevels.length, 0, 'No active levels');
    });

    // ---- Test: Level 4 base points (2026 fee revision) ----
    runTests('Level 4 Base Points (2026 Revision)', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [60, 0, 0, 0, 0];
        // Set highest level to 4
        calc.anesthesiaTimes = [0, 0, 0, 60, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        assertEquals(result.basePoints, 6500, 'Level 4 normal base (2026 revision: 6500)');
    });

    // ---- Test: Level 4 difficult base points (2026 fee revision) ----
    runTests('Level 4 Difficult Base Points (2026 Revision)', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [0, 0, 0, 60, 0];
        calc.severityLevel = 'difficult';
        
        const result = calc.calculate();
        
        assertEquals(result.basePoints, 9015, 'Level 4 difficult base (2026 revision: 9015)');
    });

    // ---- Test: Zero minutes edge case ----
    runTests('Zero Minutes Edge Case', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [0, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasOvertimeAddition = true;
        
        const result = calc.calculate();
        
        assertEquals(result.totalPoints, 0, 'Zero points even with overtime flag');
    });

    // ---- Test: Large anesthesia time ----
    runTests('Large Anesthesia Time (300 min)', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [300, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        
        const result = calc.calculate();
        
        // 300 - 120 = 180 min overtime -> ceil(180/30) = 6 blocks
        // Level 1: 6 * 1800 = 10800
        assertEquals(result.timeExtensionPoints, 10800, 'Time extension for 300 min');
    });

    // ---- Test: Results structure completeness ----
    runTests('Results Structure Completeness', () => {
        calc.resetCalculator();
        calc.anesthesiaTimes = [90, 0, 0, 0, 0];
        calc.severityLevel = 'normal';
        calc.hasEpiduralAnesthesia = true;
        calc.epiduralLocation = 'lumbar';
        calc.hasNerveBlock = true;
        calc.nerveBlockType = 'other';
        calc.hasOvertimeAddition = true;
        
        const result = calc.calculate();
        
        // Check all expected properties exist
        assert(result.totalPoints !== undefined, 'totalPoints exists');
        assert(result.basePoints !== undefined, 'basePoints exists');
        assert(result.timeExtensionPoints !== undefined, 'timeExtensionPoints exists');
        assert(result.epiduralPoints !== undefined, 'epiduralPoints exists');
        assert(result.epiduralTimeExtensionPoints !== undefined, 'epiduralTimeExtensionPoints exists');
        assert(result.nerveBlockPoints !== undefined, 'nerveBlockPoints exists');
        assert(result.totalMinutes !== undefined, 'totalMinutes exists');
        assert(result.highestLevel !== undefined, 'highestLevel exists');
        assert(Array.isArray(result.timeExtensionDetails), 'timeExtensionDetails is array');
        assert(Array.isArray(result.activeLevels), 'activeLevels is array');
    });

    // Print summary
    printResults();
}

// Export for browser usage
if (typeof window !== 'undefined') {
    window.runAllTests = runAllTests;
}
