package com.proteinlifsu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeAndStatsTest {

    private val lentil = FoodCatalog.find("green_lentil")!!
    private val rice = FoodCatalog.find("rice")!!

    @Test fun `tarif 100 g degeri malzeme toplamindan`() {
        // 200 g mercimek + 100 g pirinç = 300 g; protein 18.04 + 2.69 = 20.73 → 100 g'da 6.91
        val r = (Recipe.calculate(listOf(Ingredient(lentil, 200f), Ingredient(rice, 100f)), null) as Validated.Ok).value
        assertEquals(300f, r.totalGrams, 0.001f)
        assertEquals(6.91f, r.per100.proteinG, 0.01f)
        assertEquals(5.40f, r.per100.fiberG, 0.01f)
    }

    @Test fun `pismis agirlik suyu hesaba katar`() {
        // Aynı malzemeler pişince 600 g olursa değerler yarıya iner.
        val r = (Recipe.calculate(listOf(Ingredient(lentil, 200f), Ingredient(rice, 100f)), 600f) as Validated.Ok).value
        assertEquals(3.455f, r.per100.proteinG, 0.01f)
    }

    @Test fun `tarif hatalari`() {
        assertEquals(InputError.EMPTY, (Recipe.calculate(emptyList(), null) as Validated.Invalid).error)
        val whey = FoodCatalog.find("whey")!!
        // 50 g whey ~29 g protein; 10 g pişmiş ağırlığa bölünürse 100 g'da 100 g'ı aşar
        assertEquals(InputError.TOO_SMALL, (Recipe.calculate(listOf(Ingredient(whey, 50f)), 10f) as Validated.Invalid).error)
        // Whey 50 g sınırı tarifte de geçerli
        assertTrue(Recipe.calculate(listOf(Ingredient(whey, 60f)), null) is Validated.Invalid)
        assertTrue(Recipe.validateCookedWeight("0") is Validated.Invalid)
        assertTrue(Recipe.validateCookedWeight("") is Validated.Ok)
    }

    @Test fun `donem istatistigi kayitsiz gunleri sifir sayar ve gelecegi atlar`() {
        val goals = Goals(100f, 30f, 2000)
        val sums = listOf(
            DaySummary(10, DayTotals(120f, 30f, 2000)),
            DaySummary(11, DayTotals(50f, 15f, 1000)),
        )
        // 10..16 aralığı, bugün 12 → 3 gün (10, 11, 12)
        val st = Stats.period(sums, goals, 10, 16, today = 12)
        assertEquals(3, st.days)
        assertEquals(2, st.loggedDays)
        assertEquals(1, st.proteinHitDays)
        assertEquals(1, st.fiberHitDays)
        assertEquals((1.2f + 0.5f + 0f) / 3f, st.avgProteinRatio, 0.001f)
    }
}
