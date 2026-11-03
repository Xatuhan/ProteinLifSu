package com.proteinlifsu.domain

import kotlin.math.roundToInt

/** Tüm sayısal girdiler için tek kaynaklı sınırlar. UI, domain ve repository aynı değerleri kullanır. */
object InputLimits {
    const val MAX_FOOD_GRAMS = 5000f
    const val MAX_WATER_ML = 1_000

    const val MIN_PROTEIN_GOAL_G = 1f
    const val MAX_PROTEIN_GOAL_G = 1000f
    const val MIN_FIBER_GOAL_G = 1f
    const val MAX_FIBER_GOAL_G = 300f
    const val MIN_WATER_GOAL_ML = 100
    const val MAX_WATER_GOAL_ML = 20_000

    /** Metin kutusuna yazılabilecek en fazla karakter ("12345,67" = 8). */
    const val MAX_INPUT_CHARS = 8
    const val MAX_DECIMALS = 2

    /** Besin adı gibi serbest metin alanlarının üst sınırı. */
    const val MAX_LABEL_CHARS = 64

    /** Tarif: en fazla malzeme sayısı ve pişmiş toplam ağırlık. */
    const val MAX_INGREDIENTS = 40
    const val MAX_RECIPE_GRAMS = 20_000f

    /** Kullanıcının ekleyebileceği en fazla özel besin (tablo sınırsız büyümesin). */
    const val MAX_CUSTOM_FOODS = 500
}

enum class InputError { EMPTY, NOT_A_NUMBER, NOT_POSITIVE, TOO_SMALL, TOO_LARGE }

sealed interface Validated<out T> {
    data class Ok<T>(val value: T) : Validated<T>
    data class Invalid(val error: InputError) : Validated<Nothing>
}

enum class WaterUnit { ML, LITER }

/**
 * Katı girdi doğrulaması. Kabul edilen tek biçim: en fazla 6 tam basamak, isteğe bağlı tek bir
 * ondalık ayırıcı (`,` ya da `.`) ve en fazla 2 ondalık basamak. Böylece eksi işareti, üs
 * gösterimi ("1e9"), "NaN", "Infinity", boşluk ve SQL/metin parçaları daha ayrıştırma
 * aşamasında reddedilir; ayrıştırılan değer yine de sonlu mu ve sınırlar içinde mi diye denetlenir.
 */
object InputValidator {

    private val STRICT_DECIMAL = Regex("^[0-9]{1,6}([.,][0-9]{1,2})?$")

    /**
     * UI filtresi: kullanıcı yazarken yalnızca rakam ve tek ayırıcıya izin verir, uzunluğu ve
     * ondalık basamağı sınırlar. Geçersiz bir tuş vuruşu metni değiştirmez.
     */
    fun sanitizeDecimalInput(raw: String): String {
        val out = StringBuilder(raw.length.coerceAtMost(InputLimits.MAX_INPUT_CHARS))
        var seenSeparator = false
        var decimals = 0
        var integers = 0
        for (c in raw) {
            if (out.length >= InputLimits.MAX_INPUT_CHARS) break
            when {
                c in '0'..'9' -> {
                    if (seenSeparator) {
                        if (decimals >= InputLimits.MAX_DECIMALS) continue
                        decimals++
                    } else {
                        if (integers >= 6) continue
                        integers++
                    }
                    out.append(c)
                }
                (c == ',' || c == '.') && !seenSeparator -> {
                    seenSeparator = true
                    out.append(',')
                }
                else -> Unit // eksi, harf, boşluk, 'e' vb. sessizce yok sayılır
            }
        }
        return out.toString()
    }

    /** Katı ayrıştırma; geçersizse null. */
    fun parseDecimal(raw: String): Float? {
        val trimmed = raw.trim()
        if (!STRICT_DECIMAL.matches(trimmed)) return null
        val value = trimmed.replace(',', '.').toFloatOrNull() ?: return null
        return if (value.isFinite()) value else null
    }

    /** [max]: besine özel tek kayıt sınırı (ör. whey 50 g); genel sınırı asla aşamaz. */
    fun validateFoodGrams(raw: String, max: Float = InputLimits.MAX_FOOD_GRAMS): Validated<Float> {
        if (raw.isBlank()) return Validated.Invalid(InputError.EMPTY)
        val v = parseDecimal(raw) ?: return Validated.Invalid(InputError.NOT_A_NUMBER)
        return checkFoodGrams(v, max)
    }

    fun checkFoodGrams(grams: Float, max: Float = InputLimits.MAX_FOOD_GRAMS): Validated<Float> {
        val limit = if (max.isFinite() && max > 0f) minOf(max, InputLimits.MAX_FOOD_GRAMS) else InputLimits.MAX_FOOD_GRAMS
        return when {
            !grams.isFinite() -> Validated.Invalid(InputError.NOT_A_NUMBER)
            grams <= 0f -> Validated.Invalid(InputError.NOT_POSITIVE)
            grams > limit -> Validated.Invalid(InputError.TOO_LARGE)
            else -> Validated.Ok(grams)
        }
    }

    fun validateWater(raw: String, unit: WaterUnit): Validated<Int> {
        if (raw.isBlank()) return Validated.Invalid(InputError.EMPTY)
        val v = parseDecimal(raw) ?: return Validated.Invalid(InputError.NOT_A_NUMBER)
        val ml = when (unit) {
            WaterUnit.ML -> v
            WaterUnit.LITER -> v * 1000f
        }
        // Float -> Int dönüşümünden ÖNCE sınır denetimi: taşma imkânsız.
        if (!ml.isFinite()) return Validated.Invalid(InputError.NOT_A_NUMBER)
        if (ml > InputLimits.MAX_WATER_ML) return Validated.Invalid(InputError.TOO_LARGE)
        return checkWaterMl(ml.roundToInt())
    }

    fun checkWaterMl(ml: Int): Validated<Int> = when {
        ml <= 0 -> Validated.Invalid(InputError.NOT_POSITIVE)
        ml > InputLimits.MAX_WATER_ML -> Validated.Invalid(InputError.TOO_LARGE)
        else -> Validated.Ok(ml)
    }

    fun validateGoal(metric: Metric, raw: String, waterUnit: WaterUnit = WaterUnit.ML): Validated<Float> {
        if (raw.isBlank()) return Validated.Invalid(InputError.EMPTY)
        val v = parseDecimal(raw) ?: return Validated.Invalid(InputError.NOT_A_NUMBER)
        val value = if (metric == Metric.WATER && waterUnit == WaterUnit.LITER) v * 1000f else v
        return checkGoal(metric, value)
    }

    fun checkGoal(metric: Metric, value: Float): Validated<Float> {
        if (!value.isFinite()) return Validated.Invalid(InputError.NOT_A_NUMBER)
        val (min, max) = when (metric) {
            Metric.PROTEIN -> InputLimits.MIN_PROTEIN_GOAL_G to InputLimits.MAX_PROTEIN_GOAL_G
            Metric.FIBER -> InputLimits.MIN_FIBER_GOAL_G to InputLimits.MAX_FIBER_GOAL_G
            Metric.WATER -> InputLimits.MIN_WATER_GOAL_ML.toFloat() to InputLimits.MAX_WATER_GOAL_ML.toFloat()
        }
        return when {
            value <= 0f -> Validated.Invalid(InputError.NOT_POSITIVE)
            value < min -> Validated.Invalid(InputError.TOO_SMALL)
            value > max -> Validated.Invalid(InputError.TOO_LARGE)
            else -> Validated.Ok(value)
        }
    }

    /** 100 g başına besin değeri (etiketten): 0 serbest, en fazla 100 g. */
    fun validatePer100(raw: String): Validated<Float> {
        if (raw.isBlank()) return Validated.Ok(0f)
        val v = parseDecimal(raw) ?: return Validated.Invalid(InputError.NOT_A_NUMBER)
        return checkPer100(v)
    }

    fun checkPer100(v: Float): Validated<Float> = when {
        !v.isFinite() -> Validated.Invalid(InputError.NOT_A_NUMBER)
        v < 0f -> Validated.Invalid(InputError.NOT_POSITIVE)
        v > 100f -> Validated.Invalid(InputError.TOO_LARGE)
        else -> Validated.Ok(v)
    }

    /** İsteğe bağlı porsiyon gramı: boş = yok. */
    fun validateOptionalGrams(raw: String): Validated<Float?> {
        if (raw.isBlank()) return Validated.Ok(null)
        return when (val r = validateFoodGrams(raw)) {
            is Validated.Ok -> Validated.Ok(r.value)
            is Validated.Invalid -> r
        }
    }

    /**
     * Besin adı: kontrol karakterleri atılır, boşluklar sadeleştirilir, en fazla
     * [InputLimits.MAX_LABEL_CHARS] karakter. Ad yalnızca metin olarak saklanır ve gösterilir;
     * SQL'e her zaman bağlı değişkenle gider.
     */
    fun sanitizeName(raw: String): String {
        val sb = StringBuilder()
        var lastSpace = false
        for (c in raw) {
            if (sb.length >= InputLimits.MAX_LABEL_CHARS) break
            when {
                c.isISOControl() || c.code == 0x2028 || c.code == 0x2029 -> Unit
                c.isWhitespace() -> {
                    if (!lastSpace && sb.isNotEmpty()) sb.append(' ')
                    lastSpace = true
                }
                else -> {
                    sb.append(c)
                    lastSpace = false
                }
            }
        }
        return sb.toString()
    }

    fun validateName(raw: String): Validated<String> {
        val name = sanitizeName(raw).trim()
        return if (name.isEmpty()) Validated.Invalid(InputError.EMPTY) else Validated.Ok(name)
    }
}

enum class InputField { FOOD_GRAMS, WATER, GOAL_PROTEIN, GOAL_FIBER, GOAL_WATER, PER_100, RECIPE_WEIGHT, NAME }
