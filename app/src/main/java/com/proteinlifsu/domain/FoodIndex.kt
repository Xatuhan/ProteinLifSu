package com.proteinlifsu.domain

/**
 * USDA tam veri setinin (≈13 400 besin) bellek içi arama dizini. Nesne yerine paralel diziler
 * tutulur (bellek ve ayrıştırma hızı için); `Food` yalnız sonuç olarak istenen satırlar için üretilir.
 *
 * Dosya biçimi (tools/usda/build_foods.py üretir), sekmeyle ayrılmış:
 *   fdcId  kaynak  adEN  adTR  protein100g  lif100g(boş=yok)  ölçüler("g~en~tr;...")
 * '#' ile başlayan satırlar yorumdur. Bozuk ya da sınır dışı satırlar sessizce atlanır
 * (dosya uygulamanın kendi varlığı olsa da girdi doğrulanır).
 */
class FoodIndex private constructor(
    private val ids: IntArray,
    private val namesEn: Array<String>,
    private val namesTr: Array<String>,
    private val keys: Array<String>,
    private val protein: FloatArray,
    private val fiber: FloatArray,
    private val portionsRaw: Array<String>,
    private val water: FloatArray,
    private val liquid: BooleanArray,
) {
    val size: Int get() = ids.size

    private val positionById: HashMap<Int, Int> by lazy {
        HashMap<Int, Int>(ids.size * 2).also { m -> ids.forEachIndexed { i, id -> m[id] = i } }
    }

    fun find(foodId: String?): Food? {
        if (foodId == null || !foodId.startsWith(Food.USDA_PREFIX)) return null
        val fdc = foodId.removePrefix(Food.USDA_PREFIX).toIntOrNull() ?: return null
        val pos = positionById[fdc] ?: return null
        return foodAt(pos)
    }

    /**
     * Sorgudaki tüm kelimeler (Türkçe ya da İngilizce adda) geçmeli. Sıralama: ad sorguyla
     * başlıyorsa önce, sonra bir kelimesi ilk terimle başlıyorsa, sonra kısa ad önce.
     */
    fun search(query: String, limit: Int = 60): List<Food> {
        val q = FoodCatalog.normalize(query.trim().take(InputLimits.MAX_LABEL_CHARS))
        val terms = q.split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()
        val first = terms[0]
        val hits = ArrayList<Long>()
        for (i in keys.indices) {
            val k = keys[i]
            var ok = true
            for (t in terms) {
                if (!k.contains(t)) {
                    ok = false
                    break
                }
            }
            if (!ok) continue
            val score = when {
                k.startsWith(q) || k.contains(SEP + q) -> 0
                k.startsWith(first) || k.contains(" $first") || k.contains(SEP + first) -> 1
                else -> 2
            }
            val len = minOf(namesTr[i].length, 0xFFFF)
            hits.add((score.toLong() shl 48) or (len.toLong() shl 24) or i.toLong())
        }
        hits.sort()
        val n = minOf(limit, hits.size)
        return List(n) { foodAt((hits[it] and 0xFFFFFF).toInt()) }
    }

    private fun foodAt(i: Int): Food = Food(
        id = Food.USDA_PREFIX + ids[i],
        nameTr = namesTr[i],
        nameEn = namesEn[i],
        proteinPer100g = protein[i],
        fiberPer100g = if (fiber[i].isNaN()) 0f else fiber[i],
        portions = emptyMap(),
        fdcId = ids[i],
        household = parsePortions(portionsRaw[i]),
        fiberKnown = !fiber[i].isNaN(),
        autoTranslated = true,
        waterPer100g = if (water[i].isNaN()) null else water[i],
        liquid = liquid[i] && !water[i].isNaN(),
    )

    companion object {
        private const val SEP = "\u0001"

        fun parse(lines: Sequence<String>): FoodIndex {
            val ids = ArrayList<Int>(14_000)
            val en = ArrayList<String>(14_000)
            val tr = ArrayList<String>(14_000)
            val keys = ArrayList<String>(14_000)
            val p = ArrayList<Float>(14_000)
            val f = ArrayList<Float>(14_000)
            val por = ArrayList<String>(14_000)
            val wat = ArrayList<Float>(14_000)
            val liq = ArrayList<Boolean>(14_000)
            for (line in lines) {
                if (line.isEmpty() || line[0] == '#') continue
                val c = line.split('\t')
                if (c.size < 6) continue
                val id = c[0].toIntOrNull() ?: continue
                if (id <= 0) continue
                val nameEn = c[2].take(200)
                val nameTr = c[3].take(200).ifEmpty { nameEn }
                if (nameEn.isEmpty()) continue
                val protein = c[4].toFloatOrNull() ?: continue
                val fiber = if (c[5].isEmpty()) Float.NaN else (c[5].toFloatOrNull() ?: continue)
                if (!protein.isFinite() || protein < 0f || protein > 100f) continue
                if (!fiber.isNaN() && (fiber < 0f || fiber > 100f || protein + fiber > 100f)) continue
                ids.add(id)
                en.add(nameEn)
                tr.add(nameTr)
                keys.add(FoodCatalog.normalize(nameTr) + SEP + FoodCatalog.normalize(nameEn))
                p.add(protein)
                f.add(fiber)
                por.add(if (c.size > 6) c[6] else "")
                val w = c.getOrNull(7)?.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..100f }
                wat.add(w ?: Float.NaN)
                liq.add(c.getOrNull(8) == "1")
            }
            return FoodIndex(
                ids.toIntArray(), en.toTypedArray(), tr.toTypedArray(), keys.toTypedArray(),
                p.toFloatArray(), f.toFloatArray(), por.toTypedArray(),
                wat.toFloatArray(), liq.toBooleanArray(),
            )
        }

        val EMPTY: FoodIndex = parse(emptySequence())

        fun parsePortions(raw: String): List<HouseholdPortion> {
            if (raw.isEmpty()) return emptyList()
            val out = ArrayList<HouseholdPortion>(4)
            for (item in raw.split(';')) {
                val parts = item.split('~')
                if (parts.size != 3) continue
                val g = parts[0].toFloatOrNull() ?: continue
                if (!g.isFinite() || g <= 0f || g > InputLimits.MAX_FOOD_GRAMS) continue
                out.add(HouseholdPortion(g, parts[1].take(80), parts[2].take(80)))
                if (out.size >= 4) break
            }
            return out
        }
    }
}
