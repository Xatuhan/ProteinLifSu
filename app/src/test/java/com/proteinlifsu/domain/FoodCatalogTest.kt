package com.proteinlifsu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCatalogTest {

    @Test fun `sozluk butunlugu`() {
        val ids = FoodCatalog.foods.map { it.id }
        assertEquals("kimlikler benzersiz olmalı", ids.size, ids.toSet().size)
        for (f in FoodCatalog.foods) {
            assertTrue(f.nameTr, f.proteinPer100g.isFinite() && f.proteinPer100g >= 0f)
            assertTrue(f.nameTr, f.fiberPer100g.isFinite() && f.fiberPer100g >= 0f)
            assertTrue("${f.nameTr}: 100 g'da 100 g'dan fazla besin olamaz", f.proteinPer100g + f.fiberPer100g <= 100f)
            assertTrue("${f.nameTr}: en az bir hazır ölçü", f.portions.isNotEmpty())
            assertTrue(f.nameTr, PortionType.GRAM !in f.portions)
            f.portions.values.forEach { g ->
                assertTrue("${f.nameTr}: $g g", g > 0 && g <= f.maxGramsPerEntry)
            }
            assertEquals(PortionType.GRAM, f.availablePortions.last())
            // Elle seçilmişlerde USDA kimliği, Türk yemeklerinde tarif notu zorunlu.
            assertTrue("${f.nameTr}: kaynak", (f.fdcId ?: 0) > 0 || (f.id.startsWith("tr_") && f.noteTr != null))
            assertTrue("${f.nameTr}: İngilizce ad", f.nameEn.isNotBlank())
            assertTrue(f.nameTr, !f.isCustom)
        }
    }

    @Test fun `ortalama tabak seffaf karsilik`() {
        val chicken = FoodCatalog.find("chicken_breast")!!
        val g = chicken.gramsFor(PortionType.MEDIUM_PLATE)!!
        val n = chicken.nutrientsFor(g.toFloat())
        assertEquals(180, g)
        assertEquals(55.84f, n.proteinG, 0.01f)
        assertEquals(0f, n.fiberG, 0.0f)
    }

    @Test fun `gecersiz gram besin hesabini bozmaz`() {
        val oats = FoodCatalog.find("oats")!!
        assertEquals(0f, oats.nutrientsFor(Float.NaN).proteinG)
        assertEquals(0f, oats.nutrientsFor(-50f).proteinG)
        // Üst sınır kırpılır
        assertEquals(oats.nutrientsFor(InputLimits.MAX_FOOD_GRAMS).proteinG, oats.nutrientsFor(1e30f).proteinG)
    }

    @Test fun `whey tek seferde en fazla 50 g`() {
        val whey = FoodCatalog.find("whey")!!
        assertEquals(50f, whey.maxGramsPerEntry)
        assertTrue(InputValidator.checkFoodGrams(51f, whey.maxGramsPerEntry) is Validated.Invalid)
        // Hesap da kırpılır: 200 g istense bile en fazla 50 g'lık değer.
        assertEquals(whey.nutrientsFor(50f).proteinG, whey.nutrientsFor(200f).proteinG)
    }

    @Test fun `kendi besinlerim aramada once gelir ve iki dilde aranir`() {
        val mine = Food("custom:1", "Mercimek çorbam", "Mercimek çorbam", 4f, 2f, emptyMap(), isCustom = true)
        val r = FoodCatalog.search("mercimek", listOf(mine))
        assertEquals("custom:1", r.first().id)
        assertTrue(r.any { it.id == "green_lentil" })
        assertTrue(FoodCatalog.search("chicken").any { it.id == "chicken_breast" })
    }

    @Test fun `turkce harf duyarsiz arama`() {
        assertTrue(FoodCatalog.search("gogsu").any { it.id == "chicken_breast" })
        assertTrue(FoodCatalog.search("GÖĞSÜ").any { it.id == "chicken_breast" })
        assertTrue(FoodCatalog.search("ISPANAK").any { it.id == "spinach" })
        assertTrue(FoodCatalog.search("ıspanak").any { it.id == "spinach" })
        assertTrue(FoodCatalog.search("cekirdek_yok_boyle").isEmpty())
        assertEquals(FoodCatalog.foods.size, FoodCatalog.search("  ").size)
        assertTrue(FoodCatalog.foods.size > 150)
    }
}
