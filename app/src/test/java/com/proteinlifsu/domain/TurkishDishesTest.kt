package com.proteinlifsu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurkishDishesTest {

    private val dishes = TurkishDishes.foods

    @Test fun `tum yemekler tutarli`() {
        assertTrue("en az 100 Türk yemeği", dishes.size >= 100)
        val ids = (FoodCatalog.foods.map { it.id })
        assertEquals("kimlikler benzersiz", ids.size, ids.toSet().size)
        for (d in dishes) {
            val w = d.waterPer100g!!
            assertTrue(d.nameTr, d.proteinPer100g in 0f..100f && d.fiberPer100g in 0f..100f && w in 0f..100f)
            // 100 g'da protein + lif + su 100 g'ı aşamaz (kütle dengesi).
            assertTrue(d.nameTr, d.proteinPer100g + d.fiberPer100g + w <= 100.01f)
            assertTrue("${d.nameTr}: ölçü", d.portions.isNotEmpty())
            assertTrue(d.nameEn.isNotBlank())
            assertTrue(d.id.startsWith("tr_"))
        }
    }

    @Test fun `corbanin suyu su hedefine eklenir`() {
        val soup = FoodCatalog.find("tr_mercimek_corbasi")!!
        assertTrue(soup.liquid)
        assertEquals(200, soup.gramsFor(PortionType.SMALL_BOWL))
        // 250 g kase × %84,9 su ≈ 212 ml
        val ml = soup.waterMlFor(250f)
        assertTrue("ml=$ml", ml in 205..220)
        // Katı yemekte suyun hedefe eklenmesi yok.
        assertEquals(0, FoodCatalog.find("tr_adana_kebap")!!.waterMlFor(180f))
        // Çay neredeyse tamamen su.
        assertEquals(100, FoodCatalog.find("tr_cay")!!.waterMlFor(100f))
        // Geçersiz gram suyu bozmaz.
        assertEquals(0, soup.waterMlFor(Float.NaN))
        assertEquals(0, soup.waterMlFor(-5f))
    }

    @Test fun `turkce arama yemekleri bulur`() {
        assertTrue(FoodCatalog.search("mercimek corbasi").any { it.id == "tr_mercimek_corbasi" })
        assertTrue(FoodCatalog.search("İskender").any { it.id == "tr_iskender" })
        assertTrue(FoodCatalog.search("baklava").any { it.id == "tr_baklava_fistikli" })
        assertTrue(FoodCatalog.search("lentil soup").any { it.id == "tr_mercimek_corbasi" })
    }
}
