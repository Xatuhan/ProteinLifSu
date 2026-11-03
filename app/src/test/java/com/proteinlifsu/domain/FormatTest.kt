package com.proteinlifsu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {

    @Test fun grams() {
        assertEquals("0", Fmt.grams(0f))
        assertEquals("0", Fmt.grams(Float.NaN))
        assertEquals("4,5", Fmt.grams(4.5f))
        assertEquals("4.5", Fmt.grams(4.5f, '.'))
        assertEquals("4", Fmt.grams(4.04f))
        assertEquals("32", Fmt.grams(31.6f))
    }

    @Test fun water() {
        assertEquals("330 ml", Fmt.water(330))
        assertEquals("1 L", Fmt.water(1000))
        assertEquals("1,5 L", Fmt.water(1500))
        assertEquals("1.25 L", Fmt.water(1250, '.'))
        assertEquals("2,50", Fmt.liters2(2500))
        assertEquals("0,05", Fmt.liters2(50))
    }

    @Test fun `yuzde isareti dile gore`() {
        assertEquals("%47", Fmt.percent(0.47f, AppLanguage.TR))
        assertEquals("47%", Fmt.percent(0.47f, AppLanguage.EN))
        assertEquals("%0", Fmt.percent(Float.NaN, AppLanguage.TR))
    }

    @Test fun `hazir su olculeri tek kayit sinirini asmaz`() {
        WaterPresets.all.forEach { assertTrue("${it.kind}", it.ml in 1..InputLimits.MAX_WATER_ML) }
    }

    @Test fun totals() {
        val e = LogEntry("a", EntryKind.FOOD, "x", null, null, null, 0L, 100f, 10f, 2f, 0)
        val w = LogEntry("b", EntryKind.WATER, "y", null, null, null, 0L, 0f, 0f, 0f, 500)
        assertEquals(DayTotals(10f, 2f, 500), listOf(e, w).totals())
    }
}
