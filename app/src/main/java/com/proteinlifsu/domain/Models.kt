package com.proteinlifsu.domain

import androidx.compose.runtime.Immutable

enum class Metric { PROTEIN, FIBER, WATER }

enum class EntryKind { FOOD, WATER }

enum class AppLanguage { TR, EN }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Immutable
data class AppSettings(val language: AppLanguage, val theme: ThemeMode = ThemeMode.SYSTEM)

/** Yemek ölçüleri. GRAM, kullanıcının elle yazdığı gramajdır. Etiketler dil dosyasındadır. */
enum class PortionType {
    SMALL_BOWL,
    MEDIUM_BOWL,
    LARGE_BOWL,
    MEDIUM_PLATE,
    LARGE_PLATE,
    PIECE,
    GRAM,
    /** USDA ev ölçüsü (ör. "1 cup"); adı kayıtta ayrıca saklanır. */
    HOUSEHOLD,
}

/** "1 Adet/Porsiyon" ölçüsünün besine özgü adı. */
enum class PieceUnit {
    SERVING, PIECE, MEDIUM_PIECE, SLICE, GLASS, CAN, SCOOP, HANDFUL, TBSP1, TBSP2, FIVE_PIECES, MEATBALL_SERVING,
    TEA_GLASS, COFFEE_CUP, MUG, BOWL, PIDE,
}

@Immutable
data class Nutrients(val proteinG: Float, val fiberG: Float)

@Immutable
data class Goals(
    val proteinG: Float = DEFAULT_PROTEIN_G,
    val fiberG: Float = DEFAULT_FIBER_G,
    val waterMl: Int = DEFAULT_WATER_ML,
) {
    companion object {
        const val DEFAULT_PROTEIN_G = 120f
        const val DEFAULT_FIBER_G = 30f
        const val DEFAULT_WATER_ML = 2500
    }
}

@Immutable
data class DayTotals(val proteinG: Float = 0f, val fiberG: Float = 0f, val waterMl: Int = 0)

/** Takvim için tek günün toplamı. */
@Immutable
data class DaySummary(val epochDay: Long, val totals: DayTotals)

/** Günlük akıştaki tek kayıt. Metinler (ölçü adı, saat) arayüzde dile göre üretilir. */
@Immutable
data class LogEntry(
    val id: String,
    val kind: EntryKind,
    /** Kaydedildiği andaki ad (özel besin silinse bile görünür kalsın diye). */
    val label: String,
    val foodId: String?,
    val portion: PortionType?,
    /** HOUSEHOLD ölçüsünde ölçünün adı (kaydedildiği dilde). */
    val portionLabel: String? = null,
    val createdAt: Long,
    val grams: Float,
    val proteinG: Float,
    val fiberG: Float,
    val waterMl: Int,
)

/**
 * Günlük kayıt grubu: aynı şeyin tekrarları tek satırda. Tüm su kayıtları tek grup ("Su");
 * besinler aynı besin kimliğine (yoksa ada) göre gruplanır. [entries] en yeniden eskiye.
 */
@Immutable
data class LogGroup(
    val key: String,
    val kind: EntryKind,
    val entries: List<LogEntry>,
    val proteinG: Float,
    val fiberG: Float,
    val waterMl: Int,
    val grams: Float,
) {
    val first: LogEntry get() = entries.first()
    val count: Int get() = entries.size
}

/** Girdi sırası korunur (en yeni grup önce). */
fun List<LogEntry>.grouped(): List<LogGroup> {
    val order = LinkedHashMap<String, MutableList<LogEntry>>()
    for (e in this) {
        val key = when (e.kind) {
            EntryKind.WATER -> "water"
            EntryKind.FOOD -> "food:" + (e.foodId ?: e.label)
        }
        order.getOrPut(key) { ArrayList() }.add(e)
    }
    return order.map { (key, list) ->
        var p = 0.0
        var f = 0.0
        var w = 0L
        var g = 0.0
        for (e in list) {
            p += e.proteinG; f += e.fiberG; w += e.waterMl; g += e.grams
        }
        LogGroup(key, list.first().kind, list, p.toFloat(), f.toFloat(), w.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), g.toFloat())
    }
}

fun List<LogEntry>.totals(): DayTotals {
    var p = 0.0
    var f = 0.0
    var w = 0L
    for (e in this) {
        p += e.proteinG
        f += e.fiberG
        w += e.waterMl
    }
    return DayTotals(p.toFloat(), f.toFloat(), w.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
}

/** Bir dönemin hedef tutturma özeti (takvim). Hedefler = şu anki hedefler. */
@Immutable
data class PeriodStats(
    val days: Int,
    val loggedDays: Int,
    val avgProteinRatio: Float,
    val avgFiberRatio: Float,
    val avgWaterRatio: Float,
    val proteinHitDays: Int,
    val fiberHitDays: Int,
    val waterHitDays: Int,
)

object Stats {
    /**
     * [from]..[to] (dahil) aralığındaki günler için ortalama hedef oranı ve hedefin tutturulduğu
     * gün sayısı. Kayıt olmayan günler 0 sayılır (gün hiç atlanmaz); gelecek günler hariç tutulur.
     */
    fun period(summaries: List<DaySummary>, goals: Goals, from: Long, to: Long, today: Long): PeriodStats {
        val end = minOf(to, today)
        if (end < from) return PeriodStats(0, 0, 0f, 0f, 0f, 0, 0, 0)
        val byDay = summaries.associateBy { it.epochDay }
        var days = 0
        var logged = 0
        var p = 0.0
        var f = 0.0
        var w = 0.0
        var ph = 0
        var fh = 0
        var wh = 0
        for (d in from..end) {
            days++
            val t = byDay[d]?.totals ?: DayTotals()
            if (byDay.containsKey(d)) logged++
            val pr = ratio(t.proteinG, goals.proteinG)
            val fr = ratio(t.fiberG, goals.fiberG)
            val wr = ratio(t.waterMl.toFloat(), goals.waterMl.toFloat())
            p += pr; f += fr; w += wr
            if (pr >= 1f) ph++
            if (fr >= 1f) fh++
            if (wr >= 1f) wh++
        }
        return PeriodStats(
            days = days,
            loggedDays = logged,
            avgProteinRatio = (p / days).toFloat(),
            avgFiberRatio = (f / days).toFloat(),
            avgWaterRatio = (w / days).toFloat(),
            proteinHitDays = ph,
            fiberHitDays = fh,
            waterHitDays = wh,
        )
    }

    fun ratio(value: Float, goal: Float): Float =
        if (goal > 0f && value.isFinite() && value > 0f) value / goal else 0f
}
