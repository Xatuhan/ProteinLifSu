package com.proteinlifsu.ui.i18n

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.proteinlifsu.domain.AppLanguage
import com.proteinlifsu.domain.InputError
import com.proteinlifsu.domain.InputField
import com.proteinlifsu.domain.InputLimits
import com.proteinlifsu.domain.Metric
import com.proteinlifsu.domain.PieceUnit
import com.proteinlifsu.domain.PortionType
import com.proteinlifsu.domain.WaterPresetKind
import java.util.Locale

/**
 * Uygulama içi metinler. Android kaynak dosyaları yerine Kotlin nesnesi: dil Ayarlar'dan
 * anında değişir (Activity yeniden başlamaz) ve eksik çeviri derleme hatası olur.
 * Not: Türkçe büyük harfli metinler elle yazılır, programatik büyük/küçük harf dönüşümü yok.
 */
@Immutable
abstract class AppStrings {
    abstract val lang: AppLanguage
    abstract val locale: Locale
    abstract val sep: Char

    // Genel
    abstract val today: String
    abstract val yesterday: String
    abstract val prevDay: String
    abstract val nextDay: String
    abstract val backToToday: String
    abstract val back: String
    abstract val add: String
    abstract val save: String
    abstract val cancel: String
    abstract val delete: String
    abstract val calendar: String
    abstract val settings: String

    // Göstergeler
    abstract fun metric(m: Metric): String
    abstract val editGoal: String
    abstract fun goalTitle(m: Metric): String

    // Su
    abstract val waterHint: String
    abstract val waterPlaceholderMl: String
    abstract val waterPlaceholderL: String
    abstract val waterEntryLabel: String
    abstract fun waterPreset(kind: WaterPresetKind): String
    abstract fun addPreset(label: String): String

    // Besin
    abstract val food: String
    abstract val foodSource: String
    abstract val searchHint: String
    abstract val clearSearch: String
    abstract val noMatch: String
    abstract fun per100(p: String, f: String): String
    abstract fun per100NoFiber(p: String): String
    abstract val fiberUnknown: String
    abstract val autoTranslated: String
    abstract fun groupSummary(n: Int, last: String): String
    abstract val showDetails: String
    abstract val hideDetails: String
    abstract fun waterPercent(p: String): String
    abstract fun waterFromFood(ml: Int): String
    abstract val waterWord: String
    abstract val countsAsWater: String
    abstract fun showMore(n: Int): String
    abstract fun showResults(n: Int): String
    abstract val grams: String
    abstract val previewEmpty: String
    abstract val proteinWord: String
    abstract val fiberWord: String
    abstract val addOwnFood: String
    abstract val yours: String
    abstract fun portion(p: PortionType): String
    abstract fun piece(u: PieceUnit): String

    // Günlük kayıt
    abstract val log: String
    abstract fun logCount(n: Int): String
    abstract val emptyLog: String
    abstract val emptyLogHint: String
    abstract fun deleted(label: String): String
    abstract val undo: String
    abstract val saveFailed: String
    abstract fun deleteEntry(label: String): String
    abstract val proteinShort: String
    abstract val fiberShort: String

    // Ayarlar
    abstract val language: String
    abstract val theme: String
    abstract val themeSystem: String
    abstract val themeLight: String
    abstract val themeDark: String
    abstract val myFoods: String
    abstract val noCustomFoods: String
    abstract val dataSource: String
    abstract val dataSourceBody: String
    abstract val privacy: String
    abstract val privacyBody: String

    // Takvim
    abstract val day: String
    abstract val week: String
    abstract val month: String
    abstract val average: String
    abstract fun hitDays(hit: Int, total: Int): String
    abstract fun loggedDays(n: Int, total: Int): String
    abstract val basedOnCurrentGoals: String
    abstract val openDay: String
    abstract val weekdaysShort: List<String>
    abstract val noData: String

    // Kendi besinim
    abstract val newFood: String
    abstract val foodName: String
    abstract val fromLabel: String
    abstract val fromRecipe: String
    abstract val proteinPer100: String
    abstract val fiberPer100: String
    abstract val servingOptional: String
    abstract val ingredients: String
    abstract val addIngredient: String
    abstract val cookedWeightOptional: String
    abstract fun totalWeight(g: String): String
    abstract fun resultPer100(p: String, f: String): String
    abstract val limitReached: String
    abstract val recipeEmpty: String
    abstract val cookedTooSmall: String
    abstract val labelHint: String
    abstract val recipeHint: String
    abstract val sumTooLarge: String

    abstract fun error(e: InputError, field: InputField, limit: Float? = null): String
}

object TrStrings : AppStrings() {
    override val lang = AppLanguage.TR
    override val locale: Locale = Locale.forLanguageTag("tr-TR")
    override val sep = ','

    override val today = "Bugün"
    override val yesterday = "Dün"
    override val prevDay = "Önceki gün"
    override val nextDay = "Sonraki gün"
    override val backToToday = "bugüne dön"
    override val back = "Geri"
    override val add = "Ekle"
    override val save = "Kaydet"
    override val cancel = "Vazgeç"
    override val delete = "Sil"
    override val calendar = "Takvim"
    override val settings = "Ayarlar"

    override fun metric(m: Metric) = when (m) {
        Metric.PROTEIN -> "Protein"
        Metric.FIBER -> "Lif"
        Metric.WATER -> "Su"
    }
    override val editGoal = "Hedefi düzenle"
    override fun goalTitle(m: Metric) = when (m) {
        Metric.PROTEIN -> "Günlük protein hedefi"
        Metric.FIBER -> "Günlük lif hedefi"
        Metric.WATER -> "Günlük su hedefi"
    }

    override val waterHint = "Tek dokunuşla ekle"
    override val waterPlaceholderMl = "ör. 400"
    override val waterPlaceholderL = "ör. 0,75"
    override val waterEntryLabel = "Su"
    override fun waterPreset(kind: WaterPresetKind) = when (kind) {
        WaterPresetKind.GLASS -> "1 Bardak"
        WaterPresetKind.SMALL_BOTTLE -> "Küçük Şişe"
        WaterPresetKind.BOTTLE -> "Pet Şişe / Matara"
        WaterPresetKind.LARGE_1L -> "Büyük Şişe"
    }
    override fun addPreset(label: String) = "$label ekle"

    override val food = "Besin"
    override val foodSource = "13 000+ besin · USDA"
    override val searchHint = "Ne yedin? tavuk, yulaf, nohut…"
    override val clearSearch = "Aramayı temizle"
    override val noMatch = "Eşleşen besin yok. Kendin ekleyebilirsin."
    override fun per100(p: String, f: String) = "100 g: $p g protein · $f g lif"
    override fun per100NoFiber(p: String) = "100 g: $p g protein · lif verisi yok"
    override val fiberUnknown = "lif verisi yok"
    override val autoTranslated = "Türkçe ad otomatik çeviri"
    override fun groupSummary(n: Int, last: String) = "$n kez · son $last"
    override val showDetails = "Ayrıntıları göster"
    override val hideDetails = "Ayrıntıları gizle"
    override fun waterPercent(p: String) = "%$p su"
    override fun waterFromFood(ml: Int) = "+$ml ml su"
    override val waterWord = "Su"
    override val countsAsWater = "İçindeki su, su hedefine de eklenir"
    override fun showMore(n: Int) = "Daha fazla göster ($n)"
    override fun showResults(n: Int) = "Sonuçları göster ($n)"
    override val grams = "gram"
    override val previewEmpty = "Gram gir ya da bir ölçü seç"
    override val proteinWord = "Protein"
    override val fiberWord = "Lif"
    override val addOwnFood = "+ Kendi yemeğin"
    override val yours = "Senin"
    override fun portion(p: PortionType) = when (p) {
        PortionType.SMALL_BOWL -> "Küçük Kase"
        PortionType.MEDIUM_BOWL -> "Ortalama Kase"
        PortionType.LARGE_BOWL -> "Büyük Kase"
        PortionType.MEDIUM_PLATE -> "Ortalama Tabak"
        PortionType.LARGE_PLATE -> "Büyük Tabak"
        PortionType.PIECE -> "1 Adet/Porsiyon"
        PortionType.GRAM -> "Gram"
        PortionType.HOUSEHOLD -> "Ölçü"
    }
    override fun piece(u: PieceUnit) = when (u) {
        PieceUnit.SERVING -> "1 porsiyon"
        PieceUnit.PIECE -> "1 adet"
        PieceUnit.MEDIUM_PIECE -> "1 adet (orta)"
        PieceUnit.SLICE -> "1 dilim"
        PieceUnit.GLASS -> "1 bardak (200 ml)"
        PieceUnit.CAN -> "1 kutu"
        PieceUnit.SCOOP -> "1 ölçek"
        PieceUnit.HANDFUL -> "1 avuç"
        PieceUnit.TBSP1 -> "1 yemek kaşığı"
        PieceUnit.TBSP2 -> "2 yemek kaşığı"
        PieceUnit.FIVE_PIECES -> "5 adet"
        PieceUnit.MEATBALL_SERVING -> "1 porsiyon (4 adet)"
        PieceUnit.TEA_GLASS -> "1 ince belli bardak"
        PieceUnit.COFFEE_CUP -> "1 fincan"
        PieceUnit.MUG -> "1 kupa"
        PieceUnit.BOWL -> "1 kase"
        PieceUnit.PIDE -> "1 pide"
    }

    override val log = "Günlük kayıt"
    override fun logCount(n: Int) = "$n kayıt"
    override val emptyLog = "Bu gün için kayıt yok."
    override val emptyLogHint = "Yukarıdan bir su ölçüsüne dokun ya da bir besin seç."
    override fun deleted(label: String) = "$label silindi"
    override val undo = "Geri al"
    override val saveFailed = "Kaydedilemedi: değer sınırların dışında"
    override fun deleteEntry(label: String) = "$label kaydını sil"
    override val proteinShort = "P"
    override val fiberShort = "L"

    override val language = "Dil"
    override val theme = "Görünüm"
    override val themeSystem = "Sistem"
    override val themeLight = "Açık"
    override val themeDark = "Koyu"
    override val myFoods = "Kendi yemeklerim"
    override val noCustomFoods = "Henüz eklemedin. Besin aramasının yanındaki \"Kendi yemeğin\" ile ekleyebilirsin."
    override val dataSource = "Veri kaynağı"
    override val dataSourceBody = "Besinlerin protein ve lif değerleri USDA FoodData Central'dan alınmıştır: SR Legacy (2018-04), FNDDS 2021-2023 ve Foundation Foods (2026-10), toplam 13 000'den fazla besin. Türkçe adlar sözlük tabanlı otomatik çeviridir; özgün İngilizce ad her besinde görünür. Türk yemeklerinin değerleri standart tariflerden, malzemelerin USDA verileriyle hesaplanmıştır (yaklaşıktır). Çorba ve içeceklerin içindeki su, su hedefine de eklenir. Kase/tabak gramları ortalama tahmindir."
    override val privacy = "Gizlilik"
    override val privacyBody = "Uygulama internete bağlanmaz ve hiçbir izin istemez. Tüm veriler yalnızca bu telefonda durur ve yedeklemeye dahil edilmez."

    override val day = "Gün"
    override val week = "Hafta"
    override val month = "Ay"
    override val average = "Ortalama"
    override fun hitDays(hit: Int, total: Int) = "$total günün $hit gününde hedef"
    override fun loggedDays(n: Int, total: Int) = "$total günün $n gününde kayıt var"
    override val basedOnCurrentGoals = "Oranlar şu anki hedeflerine göre hesaplanır."
    override val openDay = "Bu günü aç"
    override val weekdaysShort = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")
    override val noData = "Kayıt yok"

    override val newFood = "Kendi yemeğin"
    override val foodName = "Adı (ör. Annemin mercimek çorbası)"
    override val fromLabel = "Etiketten"
    override val fromRecipe = "Tariften"
    override val proteinPer100 = "Protein (100 g'da)"
    override val fiberPer100 = "Lif (100 g'da)"
    override val servingOptional = "1 porsiyon kaç gram? (isteğe bağlı)"
    override val ingredients = "Malzemeler"
    override val addIngredient = "Malzeme ekle"
    override val cookedWeightOptional = "Pişmiş toplam ağırlık, g (isteğe bağlı)"
    override fun totalWeight(g: String) = "Toplam: $g g"
    override fun resultPer100(p: String, f: String) = "100 g'da: $p g protein · $f g lif"
    override val limitReached = "En fazla ${InputLimits.MAX_CUSTOM_FOODS} kendi yemeğin olabilir"
    override val recipeEmpty = "En az bir malzeme ekle"
    override val cookedTooSmall = "Pişmiş ağırlık bu malzemeler için çok küçük"
    override val labelHint = "Paket etiketindeki 100 g değerlerini yaz."
    override val recipeHint = "Malzemeleri gramıyla ekle; değerler otomatik hesaplanır."
    override val sumTooLarge = "Protein + lif 100 g'ı geçemez"

    override fun error(e: InputError, field: InputField, limit: Float?): String = when (e) {
        InputError.EMPTY -> if (field == InputField.NAME) "Bir ad yaz" else "Bir değer gir"
        InputError.NOT_A_NUMBER -> "Geçerli bir sayı gir"
        InputError.NOT_POSITIVE -> "Sıfırdan büyük olmalı"
        InputError.TOO_SMALL -> when (field) {
            InputField.GOAL_PROTEIN -> "En az ${InputLimits.MIN_PROTEIN_GOAL_G.toInt()} g"
            InputField.GOAL_FIBER -> "En az ${InputLimits.MIN_FIBER_GOAL_G.toInt()} g"
            InputField.GOAL_WATER -> "En az ${InputLimits.MIN_WATER_GOAL_ML} ml"
            else -> "Çok küçük"
        }
        InputError.TOO_LARGE -> when (field) {
            InputField.FOOD_GRAMS -> "Tek seferde en fazla ${(limit ?: InputLimits.MAX_FOOD_GRAMS).toInt()} g"
            InputField.WATER -> "Tek seferde en fazla ${InputLimits.MAX_WATER_ML} ml"
            InputField.GOAL_PROTEIN -> "En fazla ${InputLimits.MAX_PROTEIN_GOAL_G.toInt()} g"
            InputField.GOAL_FIBER -> "En fazla ${InputLimits.MAX_FIBER_GOAL_G.toInt()} g"
            InputField.GOAL_WATER -> "En fazla ${InputLimits.MAX_WATER_GOAL_ML} ml"
            InputField.PER_100 -> "100 g'da en fazla 100 g olabilir"
            InputField.RECIPE_WEIGHT -> "En fazla ${InputLimits.MAX_RECIPE_GRAMS.toInt()} g"
            InputField.NAME -> "Çok uzun"
        }
    }
}

object EnStrings : AppStrings() {
    override val lang = AppLanguage.EN
    override val locale: Locale = Locale.forLanguageTag("en-US")
    override val sep = '.'

    override val today = "Today"
    override val yesterday = "Yesterday"
    override val prevDay = "Previous day"
    override val nextDay = "Next day"
    override val backToToday = "back to today"
    override val back = "Back"
    override val add = "Add"
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val calendar = "Calendar"
    override val settings = "Settings"

    override fun metric(m: Metric) = when (m) {
        Metric.PROTEIN -> "Protein"
        Metric.FIBER -> "Fiber"
        Metric.WATER -> "Water"
    }
    override val editGoal = "Edit goal"
    override fun goalTitle(m: Metric) = when (m) {
        Metric.PROTEIN -> "Daily protein goal"
        Metric.FIBER -> "Daily fiber goal"
        Metric.WATER -> "Daily water goal"
    }

    override val waterHint = "Tap to add"
    override val waterPlaceholderMl = "e.g. 400"
    override val waterPlaceholderL = "e.g. 0.75"
    override val waterEntryLabel = "Water"
    override fun waterPreset(kind: WaterPresetKind) = when (kind) {
        WaterPresetKind.GLASS -> "1 Glass"
        WaterPresetKind.SMALL_BOTTLE -> "Small Bottle"
        WaterPresetKind.BOTTLE -> "Bottle / Flask"
        WaterPresetKind.LARGE_1L -> "Large Bottle"
    }
    override fun addPreset(label: String) = "Add $label"

    override val food = "Food"
    override val foodSource = "13,000+ foods · USDA"
    override val searchHint = "What did you eat? chicken, oats…"
    override val clearSearch = "Clear search"
    override val noMatch = "No matching food. You can add your own."
    override fun per100(p: String, f: String) = "100 g: $p g protein · $f g fiber"
    override fun per100NoFiber(p: String) = "100 g: $p g protein · no fiber data"
    override val fiberUnknown = "no fiber data"
    override val autoTranslated = ""
    override fun groupSummary(n: Int, last: String) = "$n times · last $last"
    override val showDetails = "Show details"
    override val hideDetails = "Hide details"
    override fun waterPercent(p: String) = "water $p%"
    override fun waterFromFood(ml: Int) = "+$ml ml water"
    override val waterWord = "Water"
    override val countsAsWater = "Its water also counts toward your water goal"
    override fun showMore(n: Int) = "Show more ($n)"
    override fun showResults(n: Int) = "Show results ($n)"
    override val grams = "grams"
    override val previewEmpty = "Enter grams or pick a portion"
    override val proteinWord = "Protein"
    override val fiberWord = "Fiber"
    override val addOwnFood = "+ Your own food"
    override val yours = "Yours"
    override fun portion(p: PortionType) = when (p) {
        PortionType.SMALL_BOWL -> "Small Bowl"
        PortionType.MEDIUM_BOWL -> "Medium Bowl"
        PortionType.LARGE_BOWL -> "Large Bowl"
        PortionType.MEDIUM_PLATE -> "Medium Plate"
        PortionType.LARGE_PLATE -> "Large Plate"
        PortionType.PIECE -> "1 Piece/Serving"
        PortionType.GRAM -> "Grams"
        PortionType.HOUSEHOLD -> "Measure"
    }
    override fun piece(u: PieceUnit) = when (u) {
        PieceUnit.SERVING -> "1 serving"
        PieceUnit.PIECE -> "1 piece"
        PieceUnit.MEDIUM_PIECE -> "1 medium"
        PieceUnit.SLICE -> "1 slice"
        PieceUnit.GLASS -> "1 glass (200 ml)"
        PieceUnit.CAN -> "1 can"
        PieceUnit.SCOOP -> "1 scoop"
        PieceUnit.HANDFUL -> "1 handful"
        PieceUnit.TBSP1 -> "1 tbsp"
        PieceUnit.TBSP2 -> "2 tbsp"
        PieceUnit.FIVE_PIECES -> "5 pieces"
        PieceUnit.MEATBALL_SERVING -> "1 serving (4 pcs)"
        PieceUnit.TEA_GLASS -> "1 tea glass"
        PieceUnit.COFFEE_CUP -> "1 cup"
        PieceUnit.MUG -> "1 mug"
        PieceUnit.BOWL -> "1 bowl"
        PieceUnit.PIDE -> "1 pide"
    }

    override val log = "Daily log"
    override fun logCount(n: Int) = "$n entries"
    override val emptyLog = "Nothing logged for this day."
    override val emptyLogHint = "Tap a water size above or pick a food."
    override fun deleted(label: String) = "$label deleted"
    override val undo = "Undo"
    override val saveFailed = "Couldn't save: value out of range"
    override fun deleteEntry(label: String) = "Delete $label"
    override val proteinShort = "P"
    override val fiberShort = "F"

    override val language = "Language"
    override val theme = "Appearance"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val myFoods = "My foods"
    override val noCustomFoods = "None yet. Use \"Your own food\" next to the food search."
    override val dataSource = "Data source"
    override val dataSourceBody = "Protein and fiber values come from USDA FoodData Central: SR Legacy (2018-04), FNDDS 2021-2023 and Foundation Foods (2026-10), over 13,000 foods. Turkish dishes are calculated from standard recipes using USDA ingredient data (approximate). Water in soups and drinks also counts toward your water goal. Bowl/plate weights are average estimates."
    override val privacy = "Privacy"
    override val privacyBody = "The app never connects to the internet and asks for no permissions. All data stays on this phone and is excluded from backups."

    override val day = "Day"
    override val week = "Week"
    override val month = "Month"
    override val average = "Average"
    override fun hitDays(hit: Int, total: Int) = "Goal met on $hit of $total days"
    override fun loggedDays(n: Int, total: Int) = "Logged on $n of $total days"
    override val basedOnCurrentGoals = "Ratios use your current goals."
    override val openDay = "Open this day"
    override val weekdaysShort = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    override val noData = "No entries"

    override val newFood = "Your own food"
    override val foodName = "Name (e.g. Mom's lentil soup)"
    override val fromLabel = "From label"
    override val fromRecipe = "From recipe"
    override val proteinPer100 = "Protein (per 100 g)"
    override val fiberPer100 = "Fiber (per 100 g)"
    override val servingOptional = "Grams in 1 serving (optional)"
    override val ingredients = "Ingredients"
    override val addIngredient = "Add ingredient"
    override val cookedWeightOptional = "Cooked total weight, g (optional)"
    override fun totalWeight(g: String) = "Total: $g g"
    override fun resultPer100(p: String, f: String) = "Per 100 g: $p g protein · $f g fiber"
    override val limitReached = "You can have at most ${InputLimits.MAX_CUSTOM_FOODS} own foods"
    override val recipeEmpty = "Add at least one ingredient"
    override val cookedTooSmall = "Cooked weight is too small for these ingredients"
    override val labelHint = "Enter the per-100 g values from the package label."
    override val recipeHint = "Add ingredients with grams; values are calculated."
    override val sumTooLarge = "Protein + fiber can't exceed 100 g"

    override fun error(e: InputError, field: InputField, limit: Float?): String = when (e) {
        InputError.EMPTY -> if (field == InputField.NAME) "Enter a name" else "Enter a value"
        InputError.NOT_A_NUMBER -> "Enter a valid number"
        InputError.NOT_POSITIVE -> "Must be greater than zero"
        InputError.TOO_SMALL -> when (field) {
            InputField.GOAL_PROTEIN -> "At least ${InputLimits.MIN_PROTEIN_GOAL_G.toInt()} g"
            InputField.GOAL_FIBER -> "At least ${InputLimits.MIN_FIBER_GOAL_G.toInt()} g"
            InputField.GOAL_WATER -> "At least ${InputLimits.MIN_WATER_GOAL_ML} ml"
            else -> "Too small"
        }
        InputError.TOO_LARGE -> when (field) {
            InputField.FOOD_GRAMS -> "At most ${(limit ?: InputLimits.MAX_FOOD_GRAMS).toInt()} g at once"
            InputField.WATER -> "At most ${InputLimits.MAX_WATER_ML} ml at once"
            InputField.GOAL_PROTEIN -> "At most ${InputLimits.MAX_PROTEIN_GOAL_G.toInt()} g"
            InputField.GOAL_FIBER -> "At most ${InputLimits.MAX_FIBER_GOAL_G.toInt()} g"
            InputField.GOAL_WATER -> "At most ${InputLimits.MAX_WATER_GOAL_ML} ml"
            InputField.PER_100 -> "At most 100 g per 100 g"
            InputField.RECIPE_WEIGHT -> "At most ${InputLimits.MAX_RECIPE_GRAMS.toInt()} g"
            InputField.NAME -> "Too long"
        }
    }
}

fun stringsFor(lang: AppLanguage): AppStrings = if (lang == AppLanguage.EN) EnStrings else TrStrings

val LocalStrings = staticCompositionLocalOf<AppStrings> { TrStrings }
