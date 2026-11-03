package com.proteinlifsu.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LogGroupTest {

    private fun water(id: String, ml: Int, at: Long, label: String = "1 Bardak") =
        LogEntry(id, EntryKind.WATER, label, null, null, null, at, 0f, 0f, 0f, ml)

    private fun food(id: String, foodId: String?, label: String, p: Float, at: Long, w: Int = 0) =
        LogEntry(id, EntryKind.FOOD, label, foodId, PortionType.GRAM, null, at, 100f, p, 1f, w)

    @Test fun `ayni seyler birlesir sira korunur`() {
        // Girdi en yeniden eskiye (DAO sırası).
        val entries = listOf(
            water("w3", 300, 50),
            food("f2", "tr_mercimek_corbasi", "Mercimek Çorbası", 7f, 40, w = 212),
            water("w2", 200, 30, "Küçük Şişe"),
            food("f1", "chicken_breast", "Tavuk", 56f, 20),
            food("f0", "tr_mercimek_corbasi", "Mercimek Çorbası", 7f, 10, w = 212),
            water("w1", 200, 5),
        )
        val g = entries.grouped()
        assertEquals(listOf("water", "food:tr_mercimek_corbasi", "food:chicken_breast"), g.map { it.key })
        val waterGroup = g[0]
        assertEquals(3, waterGroup.count)
        assertEquals(700, waterGroup.waterMl)
        assertEquals("w3", waterGroup.first.id) // en yeni kayıt başta
        val soup = g[1]
        assertEquals(2, soup.count)
        assertEquals(14f, soup.proteinG)
        assertEquals(424, soup.waterMl)
        assertEquals(1, g[2].count)
        // Toplamlar gruplamayla değişmez.
        assertEquals(entries.totals().waterMl, g.sumOf { it.waterMl })
    }

    @Test fun `kimligi olmayan besin adina gore gruplanir`() {
        val g = listOf(
            food("a", null, "Eski kayıt", 1f, 2),
            food("b", null, "Eski kayıt", 1f, 1),
            food("c", null, "Başka", 1f, 0),
        ).grouped()
        assertEquals(2, g.size)
        assertEquals(2, g[0].count)
    }
}
