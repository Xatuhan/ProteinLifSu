package com.proteinlifsu.domain

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * Besin. Yerleşik besinlerde protein ve lif değerleri 100 g başına, USDA FoodData Central
 * "SR Legacy" (2018-04) veri setinden alınmıştır; [fdcId] kaynağın kimliğidir. USDA'da birebir
 * karşılığı olmayanlarda en yakın kayıt kullanılır ve [noteTr]/[noteEn] bunu açıkça yazar.
 * Ölçü gramları (kase/tabak/adet) USDA'dan değil, ortalama sofra ölçülerinden tahmindir.
 *
 * Kullanıcının eklediği besinlerde [isCustom] true, [fdcId] null'dır.
 */
@Immutable
data class Food(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val proteinPer100g: Float,
    val fiberPer100g: Float,
    /** Bu besin için anlamlı ölçüler → ortalama gram. GRAM her besinde ayrıca vardır. */
    val portions: Map<PortionType, Int>,
    val piece: PieceUnit = PieceUnit.SERVING,
    val fdcId: Int? = null,
    val noteTr: String? = null,
    val noteEn: String? = null,
    val isCustom: Boolean = false,
    /** Tek kayıtta eklenebilecek en fazla gram (ör. whey tozu 50 g). */
    val maxGramsPerEntry: Float = InputLimits.MAX_FOOD_GRAMS,
    /** USDA ev ölçüleri ("1 cup" → gram). */
    val household: List<HouseholdPortion> = emptyList(),
    /** USDA'da lif verisi yoksa false; lif 0 sayılır ve arayüz "veri yok" der. */
    val fiberKnown: Boolean = true,
    /** Ad otomatik çeviri mi (USDA tam veri seti). */
    val autoTranslated: Boolean = false,
    /** 100 g'daki su (g ≈ ml); bilinmiyorsa null. */
    val waterPer100g: Float? = null,
    /** Çorba/içecek: içindeki su, eklenince su göstergesine de yazılır. */
    val liquid: Boolean = false,
) {
    /** Arayüzde gösterilecek ölçü sırası (GRAM her zaman sonda). */
    val availablePortions: List<PortionType> =
        PortionType.entries.filter { it == PortionType.GRAM || portions.containsKey(it) }

    fun name(lang: AppLanguage): String = if (lang == AppLanguage.EN) nameEn else nameTr

    fun note(lang: AppLanguage): String? = if (lang == AppLanguage.EN) noteEn else noteTr

    fun gramsFor(portion: PortionType): Int? = portions[portion]

    fun nutrientsFor(grams: Float): Nutrients {
        val g = if (grams.isFinite() && grams > 0f) grams.coerceAtMost(maxGramsPerEntry) else 0f
        return Nutrients(
            proteinG = proteinPer100g * g / 100f,
            fiberG = fiberPer100g * g / 100f,
        )
    }

    /**
     * Bu miktardaki suyun ml karşılığı (yalnız sıvı besinlerde; 1 g su ≈ 1 ml).
     * Örn. 250 g mercimek çorbası × %85 su ≈ 212 ml.
     */
    fun waterMlFor(grams: Float): Int {
        val w = waterPer100g ?: return 0
        if (!liquid || !grams.isFinite() || grams <= 0f || !w.isFinite() || w <= 0f) return 0
        val g = grams.coerceAtMost(maxGramsPerEntry)
        return (g * w.coerceAtMost(100f) / 100f).roundToInt()
    }

    companion object {
        const val CUSTOM_PREFIX = "custom:"
        const val USDA_PREFIX = "usda:"
    }
}

@Immutable
data class HouseholdPortion(val grams: Float, val en: String, val tr: String) {
    fun label(lang: AppLanguage): String = if (lang == AppLanguage.EN) en else tr
}

object FoodCatalog {

    private fun dish(
        smallBowl: Int = 150, mediumBowl: Int = 250, largeBowl: Int = 350,
        mediumPlate: Int = 220, largePlate: Int = 320,
    ) = mapOf(
        PortionType.SMALL_BOWL to smallBowl,
        PortionType.MEDIUM_BOWL to mediumBowl,
        PortionType.LARGE_BOWL to largeBowl,
        PortionType.MEDIUM_PLATE to mediumPlate,
        PortionType.LARGE_PLATE to largePlate,
    )

    private fun bowls(small: Int, medium: Int, large: Int) = mapOf(
        PortionType.SMALL_BOWL to small,
        PortionType.MEDIUM_BOWL to medium,
        PortionType.LARGE_BOWL to large,
    )

    private fun plates(medium: Int, large: Int) = mapOf(
        PortionType.MEDIUM_PLATE to medium,
        PortionType.LARGE_PLATE to large,
    )

    private fun piece(grams: Int) = mapOf(PortionType.PIECE to grams)

    /** Elle seçilmiş temel besinler + standart tarifle hesaplanmış Türk yemekleri. */
    val foods: List<Food> by lazy { curated + TurkishDishes.foods }

    private val curated: List<Food> = listOf(
        // ---- Hayvansal protein ----
        Food("chicken_breast", "Tavuk Göğsü (fırın)", "Chicken Breast (roasted)", 31.02f, 0f, plates(180, 260) + piece(150), fdcId = 171477),
        Food("chicken_thigh", "Tavuk But (fırın, kemiksiz)", "Chicken Thigh (roasted, boneless)", 24.76f, 0f, plates(180, 260) + piece(100), PieceUnit.PIECE, fdcId = 172388),
        Food("beef", "Kırmızı Et (yağsız biftek, ızgara)", "Beef (lean sirloin, broiled)", 29.51f, 0f, plates(180, 260) + piece(150), fdcId = 173118),
        Food(
            "meatball", "Köfte (sade kıyma, ızgara)", "Meatballs (plain ground beef, broiled)", 25.93f, 0f, plates(180, 260) + piece(120), PieceUnit.MEATBALL_SERVING,
            fdcId = 174032,
            noteTr = "USDA: %85 yağsız dana kıyma köftesi; ekmek/soğan katkısı yok",
            noteEn = "USDA: 85% lean ground beef patty; no bread/onion",
        ),
        Food("salmon", "Somon (pişmiş)", "Salmon (cooked)", 22.1f, 0f, plates(180, 260) + piece(150), fdcId = 175168),
        Food("tuna", "Ton Balığı (suda, süzülmüş)", "Tuna (canned in water, drained)", 19.44f, 0f, piece(110), PieceUnit.CAN, fdcId = 173709),
        Food("egg", "Yumurta", "Egg", 12.56f, 0f, piece(50), PieceUnit.PIECE, fdcId = 171287),
        Food("yogurt", "Yoğurt (tam yağlı)", "Yogurt (whole milk)", 3.47f, 0f, bowls(100, 200, 300), fdcId = 171284),
        Food(
            "strained_yogurt", "Süzme Yoğurt", "Strained Yogurt", 9f, 0f, bowls(100, 180, 250), fdcId = 171304,
            noteTr = "USDA: Greek yoğurt, sade, tam yağlı", noteEn = "USDA: Greek yogurt, plain, whole milk",
        ),
        Food("milk", "Süt (tam yağlı)", "Milk (whole)", 3.15f, 0f, piece(200), PieceUnit.GLASS, fdcId = 171265, waterPer100g = 88.13f, liquid = true),
        Food(
            "ayran", "Ayran", "Ayran (yogurt drink)", 1.74f, 0f, piece(200), PieceUnit.GLASS,
            fdcId = 171284,
            noteTr = "Hesaplanmış: USDA yoğurdu 1:1 suyla seyreltilmiş",
            noteEn = "Derived: USDA yogurt diluted 1:1 with water",
            waterPer100g = 93.9f, liquid = true,
        ),
        Food("kefir", "Kefir (sade)", "Kefir (plain)", 3.79f, 0f, piece(200), PieceUnit.GLASS, fdcId = 170904, waterPer100g = 89.7f, liquid = true),
        Food(
            "white_cheese", "Beyaz Peynir", "White Cheese", 14.21f, 0f, piece(30), PieceUnit.SLICE, fdcId = 173420,
            noteTr = "USDA: feta peyniri", noteEn = "USDA: feta cheese",
        ),
        Food(
            "kashar", "Kaşar Peyniri", "Kashar Cheese", 24.25f, 0f, piece(20), PieceUnit.SLICE, fdcId = 170899,
            noteTr = "USDA: cheddar peyniri", noteEn = "USDA: cheddar cheese",
        ),
        Food(
            "curd", "Lor Peyniri", "Curd Cheese (lor)", 11.39f, 0f, bowls(100, 150, 200) + piece(40), PieceUnit.TBSP2,
            fdcId = 171248, noteTr = "USDA: yarım yağlı ricotta", noteEn = "USDA: part-skim ricotta",
        ),
        Food(
            "whey", "Whey Protein Tozu", "Whey Protein Powder", 58.14f, 0f, piece(30), PieceUnit.SCOOP,
            fdcId = 173177,
            noteTr = "USDA ortalaması; markaya göre çok değişir, etiketi esas al",
            noteEn = "USDA average; varies a lot by brand, check the label",
            maxGramsPerEntry = 50f,
        ),

        // ---- Baklagil ve tahıl ----
        Food("oats", "Yulaf (kuru)", "Oats (dry)", 13.15f, 10.1f, bowls(30, 50, 80), fdcId = 173904),
        Food("green_lentil", "Mercimek (haşlanmış)", "Lentils (boiled)", 9.02f, 7.9f, dish(), fdcId = 172421),
        Food("chickpea", "Nohut (haşlanmış)", "Chickpeas (boiled)", 8.86f, 7.6f, dish(), fdcId = 173757),
        Food("white_bean", "Kuru Fasulye (haşlanmış)", "White Beans (boiled)", 9.73f, 6.3f, dish(), fdcId = 175203),
        Food("bulgur", "Bulgur (pişmiş)", "Bulgur (cooked)", 3.08f, 4.5f, dish(), fdcId = 170287),
        Food("rice", "Pirinç (pişmiş)", "Rice (cooked)", 2.69f, 0.4f, dish(), fdcId = 168878),
        Food("pasta", "Makarna (pişmiş)", "Pasta (cooked)", 5.8f, 1.8f, dish(), fdcId = 169737),
        Food("ww_pasta", "Tam Buğday Makarna (pişmiş)", "Whole-wheat Pasta (cooked)", 5.99f, 3.9f, dish(), fdcId = 168910),
        Food("quinoa", "Kinoa (pişmiş)", "Quinoa (cooked)", 4.4f, 2.8f, dish(), fdcId = 168917),
        Food("hummus", "Humus", "Hummus", 7.78f, 5.5f, bowls(100, 150, 200) + piece(60), fdcId = 174289),
        Food("white_bread", "Beyaz Ekmek", "White Bread", 8.85f, 2.7f, piece(30), PieceUnit.SLICE, fdcId = 174924),
        Food("ww_bread", "Tam Buğday Ekmeği", "Whole-wheat Bread", 12.45f, 6f, piece(30), PieceUnit.SLICE, fdcId = 172688),

        // ---- Sebze ve meyve ----
        Food("salad", "Yeşil Salata (marul)", "Green Salad (romaine)", 1.23f, 2.1f, bowls(80, 150, 250) + plates(150, 250), fdcId = 169247),
        Food("tomato", "Domates", "Tomato", 0.88f, 1.2f, piece(120), PieceUnit.MEDIUM_PIECE, fdcId = 170457),
        Food("cucumber", "Salatalık", "Cucumber", 0.65f, 0.5f, piece(150), PieceUnit.MEDIUM_PIECE, fdcId = 168409),
        Food("broccoli", "Brokoli (haşlanmış)", "Broccoli (boiled)", 2.38f, 3.3f, dish(), fdcId = 169967),
        Food("spinach", "Ispanak (haşlanmış)", "Spinach (boiled)", 2.97f, 2.4f, dish(), fdcId = 168463),
        Food("apple", "Elma", "Apple", 0.26f, 2.4f, piece(180), PieceUnit.MEDIUM_PIECE, fdcId = 171688),
        Food("banana", "Muz", "Banana", 1.09f, 2.6f, piece(120), PieceUnit.MEDIUM_PIECE, fdcId = 173944),
        Food("pear", "Armut", "Pear", 0.36f, 3.1f, piece(180), PieceUnit.MEDIUM_PIECE, fdcId = 169118),
        Food("dried_apricot", "Kuru Kayısı", "Dried Apricots", 3.39f, 7.3f, piece(40), PieceUnit.FIVE_PIECES, fdcId = 173941),

        // ---- Kuruyemiş ve tohum ----
        Food("almond", "Badem", "Almonds", 21.15f, 12.5f, piece(30), PieceUnit.HANDFUL, fdcId = 170567),
        Food("walnut", "Ceviz", "Walnuts", 15.23f, 6.7f, piece(30), PieceUnit.HANDFUL, fdcId = 170187),
        Food("hazelnut", "Fındık", "Hazelnuts", 14.95f, 9.7f, piece(30), PieceUnit.HANDFUL, fdcId = 170581),
        Food("peanut", "Yer Fıstığı (kavrulmuş)", "Peanuts (dry-roasted)", 24.35f, 8.4f, piece(30), PieceUnit.HANDFUL, fdcId = 173806),
        Food("peanut_butter", "Fıstık Ezmesi", "Peanut Butter", 22.21f, 5f, piece(32), PieceUnit.TBSP2, fdcId = 172470),
        Food("chia", "Chia Tohumu", "Chia Seeds", 16.54f, 34.4f, piece(12), PieceUnit.TBSP1, fdcId = 170554),
    )

    private val byId: Map<String, Food> by lazy { foods.associateBy { it.id } }

    fun find(id: String?): Food? = id?.let(byId::get)

    /** Türkçe harfleri ASCII'ye indirip küçük harfe çevirir: "Göğüs" ≈ "gogus", "IŞIK" ≈ "isik". */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            sb.append(
                when (c) {
                    'ç', 'Ç' -> 'c'
                    'ğ', 'Ğ' -> 'g'
                    'ı', 'I', 'İ', 'i' -> 'i'
                    'ö', 'Ö' -> 'o'
                    'ş', 'Ş' -> 's'
                    'ü', 'Ü' -> 'u'
                    in 'A'..'Z' -> c + ('a' - 'A')
                    else -> c
                },
            )
        }
        return sb.toString()
    }

    private val normalized: Map<String, String> by lazy {
        foods.associate { it.id to (normalize(it.nameTr) + " " + normalize(it.nameEn)) }
    }

    private fun matchesIndexed(food: Food, terms: List<String>): Boolean {
        val k = normalized[food.id] ?: (normalize(food.nameTr) + " " + normalize(food.nameEn))
        return terms.all { k.contains(it) }
    }

    private fun matches(food: Food, q: String): Boolean =
        normalize(food.nameTr).contains(q) || normalize(food.nameEn).contains(q)

    /**
     * Kullanıcının besinleri ([custom]) önce, ardından yerleşik besinler. İki dildeki adda da
     * aranır; dil değişse de aynı sorgu çalışır.
     */
    fun search(query: String, custom: List<Food> = emptyList()): List<Food> {
        val q = normalize(query.trim().take(InputLimits.MAX_LABEL_CHARS))
        if (q.isEmpty()) return custom + foods
        val terms = q.split(' ').filter { it.isNotEmpty() }
        return custom.filter { matches(it, q) } + foods.filter { matchesIndexed(it, terms) }
    }
}
