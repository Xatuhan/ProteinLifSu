package com.proteinlifsu.domain

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class WaterPresetKind { GLASS, SMALL_BOTTLE, BOTTLE, LARGE_1L }

@Immutable
data class WaterPreset(val kind: WaterPresetKind, val ml: Int)

/** Tek seferde en fazla 1000 ml (InputLimits.MAX_WATER_ML) eklenebilir; hazır ölçüler buna uyar. */
object WaterPresets {
    val all: List<WaterPreset> = listOf(
        WaterPreset(WaterPresetKind.GLASS, 200),
        WaterPreset(WaterPresetKind.SMALL_BOTTLE, 330),
        WaterPreset(WaterPresetKind.BOTTLE, 500),
        WaterPreset(WaterPresetKind.LARGE_1L, 1000),
    )
}

/**
 * Sayı biçimi. Ondalık ayırıcı dile göre verilir (TR ',', EN '.'). String.format kullanılmaz:
 * sık çağrılan yollarda Locale/Formatter nesnesi üretmemek için elle biçimlenir.
 */
object Fmt {

    /** 10'un altındaki değerler bir ondalıkla (4,5), diğerleri tam sayı (32). */
    fun grams(value: Float, sep: Char = ','): String {
        if (!value.isFinite() || value <= 0f) return "0"
        if (value >= 10f) return value.roundToInt().toString()
        val tenths = (value * 10f).roundToInt()
        return if (tenths % 10 == 0) (tenths / 10).toString() else "${tenths / 10}$sep${tenths % 10}"
    }

    /** Sabit iki ondalıklı litre: 1250 → "1,25". */
    fun liters2(ml: Int, sep: Char = ','): String {
        val hundredths = (ml.coerceAtLeast(0) / 10.0).roundToLong()
        val whole = hundredths / 100
        val frac = (hundredths % 100).toInt()
        return "$whole$sep" + (if (frac < 10) "0$frac" else frac.toString())
    }

    /** Su miktarı: 1000 ml ve üzeri litre ("1,5 L"), altı ml ("330 ml"). */
    fun water(ml: Int, sep: Char = ','): String = if (ml >= 1000) {
        val hundredths = (ml / 10.0).roundToLong()
        val whole = hundredths / 100
        val frac = (hundredths % 100).toInt()
        when {
            frac == 0 -> "$whole L"
            frac % 10 == 0 -> "$whole$sep${frac / 10} L"
            else -> "$whole$sep" + (if (frac < 10) "0$frac" else "$frac") + " L"
        }
    } else {
        "$ml ml"
    }

    /** Yüzde: Türkçede işaret başta ("%47"), İngilizcede sonda ("47%"). */
    fun percent(fraction: Float, lang: AppLanguage = AppLanguage.TR): String {
        val n = if (fraction.isFinite()) (abs(fraction) * 100f).roundToInt() else 0
        return if (lang == AppLanguage.TR) "%$n" else "$n%"
    }
}
