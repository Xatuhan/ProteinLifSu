package com.proteinlifsu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    private fun foodError(raw: String) = (InputValidator.validateFoodGrams(raw) as? Validated.Invalid)?.error
    private fun waterOk(raw: String, unit: WaterUnit) = (InputValidator.validateWater(raw, unit) as? Validated.Ok)?.value

    @Test fun `zararli ve bozuk metinler ayristirilamaz`() {
        listOf(
            "NaN", "Infinity", "-Infinity", "-5", "+5", "1e9", "1E3", "0x10", "abc", " ", "1.2.3", "1,2,3",
            "12,345", "1234567", "5;DROP TABLE entries", "' OR 1=1 --", "١٢", "5 0", "\u0000",
        ).forEach { assertNull("'$it' reddedilmeliydi", InputValidator.parseDecimal(it)) }
    }

    @Test fun `virgul ve nokta ondalik kabul edilir`() {
        assertEquals(12.5f, InputValidator.parseDecimal("12,5"))
        assertEquals(0.75f, InputValidator.parseDecimal("0.75"))
        assertEquals(220f, InputValidator.parseDecimal("220"))
    }

    @Test fun `besin grami sinirlari`() {
        assertEquals(InputError.EMPTY, foodError(""))
        assertEquals(InputError.NOT_POSITIVE, foodError("0"))
        assertEquals(InputError.NOT_A_NUMBER, foodError("-1"))
        assertEquals(InputError.TOO_LARGE, foodError("5000,01"))
        assertTrue(InputValidator.validateFoodGrams("5000") is Validated.Ok)
        assertEquals(InputError.NOT_A_NUMBER, (InputValidator.checkFoodGrams(Float.NaN) as Validated.Invalid).error)
        assertEquals(InputError.NOT_A_NUMBER, (InputValidator.checkFoodGrams(Float.POSITIVE_INFINITY) as Validated.Invalid).error)
    }

    @Test fun `su litre ve ml donusumu ve sinirlar`() {
        assertEquals(750, waterOk("0,75", WaterUnit.LITER))
        assertEquals(400, waterOk("400", WaterUnit.ML))
        assertEquals(1_000, waterOk("1", WaterUnit.LITER))
        assertEquals(1_000, waterOk("1000", WaterUnit.ML))
        // Tek seferde en fazla 1000 ml.
        assertEquals(InputError.TOO_LARGE, (InputValidator.validateWater("1001", WaterUnit.ML) as Validated.Invalid).error)
        assertEquals(InputError.TOO_LARGE, (InputValidator.validateWater("1,01", WaterUnit.LITER) as Validated.Invalid).error)
        // 999999 L int'e çevrilmeden önce reddedilir: taşma yok.
        assertEquals(InputError.TOO_LARGE, (InputValidator.validateWater("999999", WaterUnit.LITER) as Validated.Invalid).error)
        assertEquals(InputError.NOT_POSITIVE, (InputValidator.validateWater("0,00", WaterUnit.LITER) as Validated.Invalid).error)
        assertEquals(InputError.NOT_POSITIVE, (InputValidator.checkWaterMl(Int.MIN_VALUE) as Validated.Invalid).error)
    }

    @Test fun `hedef sinirlari`() {
        assertTrue(InputValidator.validateGoal(Metric.PROTEIN, "150") is Validated.Ok)
        assertEquals(InputError.TOO_LARGE, (InputValidator.validateGoal(Metric.PROTEIN, "1001") as Validated.Invalid).error)
        assertEquals(InputError.TOO_SMALL, (InputValidator.validateGoal(Metric.WATER, "50") as Validated.Invalid).error)
        assertEquals(2500f, (InputValidator.validateGoal(Metric.WATER, "2,5", WaterUnit.LITER) as Validated.Ok).value)
    }

    @Test fun `besine ozel ust sinir`() {
        assertTrue(InputValidator.validateFoodGrams("50", 50f) is Validated.Ok)
        assertEquals(InputError.TOO_LARGE, (InputValidator.validateFoodGrams("50,5", 50f) as Validated.Invalid).error)
        // Besin sınırı genel sınırı asla aşamaz.
        assertEquals(InputError.TOO_LARGE, (InputValidator.checkFoodGrams(6000f, 99999f) as Validated.Invalid).error)
    }

    @Test fun `ozel besin adi ve 100g degerleri`() {
        assertEquals("Annemin çorbası", (InputValidator.validateName("  Annemin\n\t çorbası\u0000 ") as Validated.Ok).value)
        assertEquals(InputError.EMPTY, (InputValidator.validateName(" \u0007 ") as Validated.Invalid).error)
        assertEquals(64, InputValidator.sanitizeName("x".repeat(500)).length)
        assertTrue(InputValidator.validatePer100("") is Validated.Ok)
        assertEquals(InputError.TOO_LARGE, (InputValidator.validatePer100("100,5") as Validated.Invalid).error)
        assertEquals(InputError.NOT_A_NUMBER, (InputValidator.validatePer100("-3") as Validated.Invalid).error)
    }

    @Test fun `yazarken filtre eksi harf ve fazla basamagi atar`() {
        assertEquals("125", InputValidator.sanitizeDecimalInput("-1e25"))
        assertEquals("12,57", InputValidator.sanitizeDecimalInput("12.5.7"))
        assertEquals("0,75", InputValidator.sanitizeDecimalInput("0,7591"))
        assertEquals("123456", InputValidator.sanitizeDecimalInput("1234567890"))
        assertEquals("", InputValidator.sanitizeDecimalInput("NaN"))
        assertEquals(",", InputValidator.sanitizeDecimalInput(", "))
    }
}
