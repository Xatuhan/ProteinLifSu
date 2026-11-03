package com.proteinlifsu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FoodIndexTest {

    private val sample = """
        # yorum satırı
        1001	S	Chicken, breast, roasted	Tavuk, göğüs, kavrulmuş	31	0	85~3 oz~3 ons;140~1 cup~1 su bardağı (ABD)	65.3	0
        1006	S	Soup, lentil, ready-to-serve	Çorba, mercimek	3.5	1.5		88	1
        1002	N	Beef shish kabob with vegetables	Dana eti şiş kebap sebzeli	15.5		202~1 shishkabob~1 shishkabob
        1003	S	Bad protein row	Bozuk	150	0
        1004	S	Bad fiber row	Bozuk	60	60
        abc	S	Bad id	Bozuk	1	1
        1005	F	Lentils, boiled	Mercimek, haşlanmış	9.02	7.9	198~1 cup~1 su bardağı (ABD);x~bad~bad;0~zero~sıfır
    """.trimIndent()

    private val index = FoodIndex.parse(sample.lineSequence())

    @Test fun `bozuk ve sinir disi satirlar atlanir`() {
        assertEquals(4, index.size)
        assertNull(index.find("usda:1003"))
        assertNull(index.find("usda:1004"))
    }

    @Test fun `lif verisi yoksa bilinmiyor olarak isaretlenir`() {
        val kebab = index.find("usda:1002")!!
        assertFalse(kebab.fiberKnown)
        assertEquals(0f, kebab.fiberPer100g)
        assertTrue(kebab.autoTranslated)
    }

    @Test fun `su ve sivi sutunlari`() {
        val soup = index.find("usda:1006")!!
        assertTrue(soup.liquid)
        assertEquals(88f, soup.waterPer100g)
        assertEquals(220, soup.waterMlFor(250f))
        val chicken = index.find("usda:1001")!!
        assertFalse(chicken.liquid)
        assertEquals(0, chicken.waterMlFor(100f))
        // Eski biçimli satır (su sütunu yok) da okunur.
        assertNull(index.find("usda:1005")!!.waterPerLegacyCheck())
    }

    private fun Food.waterPerLegacyCheck(): Float? = waterPer100g

    @Test fun `olculer ayristirilir gecersizleri atlanir`() {
        val lentil = index.find("usda:1005")!!
        assertEquals(1, lentil.household.size)
        assertEquals(198f, lentil.household[0].grams)
        assertEquals("1 su bardağı (ABD)", lentil.household[0].label(AppLanguage.TR))
        assertEquals("1 cup", lentil.household[0].label(AppLanguage.EN))
    }

    @Test fun `iki dilde ve turkce harf duyarsiz arama`() {
        assertEquals("usda:1002", index.search("kebap").first().id)
        assertEquals("usda:1002", index.search("kabob").first().id)
        assertEquals("usda:1001", index.search("GÖĞÜS").first().id)
        assertEquals("usda:1005", index.search("mercimek has").first().id)
        assertTrue(index.search("   ").isEmpty())
        assertTrue(index.search("olmayanbesin").isEmpty())
    }

    @Test fun `gercek varlik dosyasi tamamen gecerli`() {
        val file = listOf(File("src/main/assets/usda_foods.tsv"), File("app/src/main/assets/usda_foods.tsv")).firstOrNull { it.exists() }
        assertNotNull("usda_foods.tsv bulunamadı", file)
        val lines = file!!.readLines(Charsets.UTF_8)
        val dataLines = lines.count { it.isNotEmpty() && !it.startsWith("#") }
        val real = FoodIndex.parse(lines.asSequence())
        // Hiçbir satır doğrulamada düşmemeli.
        assertEquals(dataLines, real.size)
        assertTrue("en az 13 000 besin", real.size >= 13_000)
        // Elle seçilmiş 43 besinle çakışma yok.
        FoodCatalog.foods.forEach { assertNull(it.nameTr, real.find("usda:${it.fdcId}")) }
        // Türkçe arama gerçekten sonuç verir.
        assertTrue(real.search("kebap").isNotEmpty())
        assertTrue(real.search("mercimek").isNotEmpty())
        assertTrue(real.search("yoğurt").isNotEmpty())
        assertTrue(real.search("baklava").isNotEmpty())
    }
}
