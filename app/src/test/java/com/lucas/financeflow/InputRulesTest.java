package com.lucas.financeflow;

import org.junit.Test;
import static org.junit.Assert.*;

public class InputRulesTest {
    @Test public void formatsCentsWhileTypingAndPasting() {
        assertEquals("0,01", MoneyInput.format("1"));
        assertEquals("0,10", MoneyInput.format("10"));
        assertEquals("1,00", MoneyInput.format("100"));
        assertEquals("1.000,00", MoneyInput.format("100000"));
        assertEquals("1.234,56", MoneyInput.format("1.234,56"));
        assertEquals("0,00", MoneyInput.format(""));
        assertEquals("999.999.999,99", MoneyInput.format("99999999999"));
        try { MoneyInput.format("100000000000"); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void acceptsSingleDateAndInclusiveRange() {
        DateRange day = new DateRange("18/09/26", "");
        assertEquals("2026-09-18", day.inicio); assertTrue(day.contem("2026-09-18")); assertFalse(day.contem("2026-09-19"));
        DateRange range = new DateRange("01/08/2026", "30/09/26");
        assertTrue(range.contem("2026-08-01")); assertTrue(range.contem("2026-09-30"));
        assertFalse(range.contem("2026-07-31")); assertFalse(range.contem("2026-10-01"));
        assertFalse(range.contem(null));
    }
    @Test public void rejectsImpossibleAndReversedDates() {
        for (String day : new String[]{"31/09/26", "29/02/26", "00/08/26", "18/13/26", "18/09/2", "", "18/09/0000"}) {
            try { new DateRange(day, ""); fail(day); } catch (IllegalArgumentException expected) { }
        }
        assertEquals("2024-02-29", DateRange.iso("29/02/24"));
        try { new DateRange("18/09/26", "01/08/26"); fail(); } catch (IllegalArgumentException expected) { }
    }
}
