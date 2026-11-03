package com.proteinlifsu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinlifsu.domain.DayTotals
import com.proteinlifsu.domain.Fmt
import com.proteinlifsu.domain.Goals
import com.proteinlifsu.domain.Metric
import com.proteinlifsu.domain.PeriodStats
import com.proteinlifsu.domain.Stats
import com.proteinlifsu.ui.components.Hairline
import com.proteinlifsu.ui.components.IconAction
import com.proteinlifsu.ui.components.ScreenTopBar
import com.proteinlifsu.ui.components.SegmentedPills
import com.proteinlifsu.ui.components.dateLabels
import com.proteinlifsu.ui.components.drawRing
import com.proteinlifsu.ui.i18n.AppStrings
import com.proteinlifsu.ui.i18n.LocalStrings
import com.proteinlifsu.ui.theme.AppIcons
import com.proteinlifsu.ui.theme.LabelStyle
import com.proteinlifsu.ui.theme.NumStyle
import com.proteinlifsu.ui.theme.Palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenDay: (Long) -> Unit,
) {
    val s = LocalStrings.current
    val st by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SidePadding,
            end = SidePadding,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "top") { ScreenTopBar(s.calendar, s.back, onBack) }
        item(key = "mode") {
            SegmentedPills(
                options = HistoryMode.entries,
                selected = st.mode,
                label = { when (it) { HistoryMode.DAY -> s.day; HistoryMode.WEEK -> s.week; HistoryMode.MONTH -> s.month } },
                accent = Palette.textPrimary,
                onSelect = viewModel::setMode,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                itemWidth = null,
            )
        }
        item(key = "nav") {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconAction(AppIcons.ChevronLeft, s.prevDay, viewModel::previous)
                Text(
                    st.title,
                    style = LabelStyle.copy(fontSize = 17.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                IconAction(AppIcons.ChevronRight, s.nextDay, viewModel::next, enabled = st.canGoNext)
            }
        }
        item(key = "body") {
            when (st.mode) {
                HistoryMode.DAY -> DayDetail(st.byDay[st.anchor] ?: DayTotals(), st.goals, st.anchor <= st.today, s) { onOpenDay(st.anchor) }
                HistoryMode.WEEK -> WeekBars(st, s, onSelect = { viewModel.select(it); viewModel.setMode(HistoryMode.DAY) })
                HistoryMode.MONTH -> MonthGrid(st, s, onSelect = { viewModel.select(it); viewModel.setMode(HistoryMode.DAY) })
            }
        }
        if (st.mode != HistoryMode.DAY) {
            item(key = "sep") { Hairline(Modifier.padding(vertical = 16.dp)) }
            item(key = "stats") { st.stats?.let { StatsBlock(it, st.goals, s) } }
        }
        item(key = "note") {
            Text(s.basedOnCurrentGoals, style = NumStyle.Small.copy(color = Palette.textMuted), modifier = Modifier.padding(top = 16.dp))
        }
    }
}

/** Dönem başlığı. Yalnız arka planda (HistoryViewModel, Default) çağrılır: ay adları ICU'dan yüklenir. */
internal fun periodTitle(mode: HistoryMode, anchor: Long, fromDay: Long, toDay: Long, today: Long, s: AppStrings): String {
    val from = LocalDate.ofEpochDay(fromDay)
    val to = LocalDate.ofEpochDay(toDay)
    return when (mode) {
        HistoryMode.DAY -> dateLabels(anchor, today, s).let { (t, sub) -> if (t == sub) t else "$t · $sub" }
        HistoryMode.WEEK -> {
            val f = DateTimeFormatter.ofPattern("d MMM", s.locale)
            "${from.format(f)} – ${to.format(f)}"
        }
        HistoryMode.MONTH -> from.format(DateTimeFormatter.ofPattern("LLLL yyyy", s.locale))
    }
}

/** Bir günün üç ölçüsü: yumuşak halkalar + değer / hedef. */
@Composable
private fun DayDetail(t: DayTotals, goals: Goals, canOpen: Boolean, s: AppStrings, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (m in Metric.entries) {
                val (value, goal, text, goalText) = when (m) {
                    Metric.PROTEIN -> Quad(t.proteinG, goals.proteinG, Fmt.grams(t.proteinG, s.sep) + " g", Fmt.grams(goals.proteinG, s.sep) + " g")
                    Metric.FIBER -> Quad(t.fiberG, goals.fiberG, Fmt.grams(t.fiberG, s.sep) + " g", Fmt.grams(goals.fiberG, s.sep) + " g")
                    Metric.WATER -> Quad(t.waterMl.toFloat(), goals.waterMl.toFloat(), Fmt.liters2(t.waterMl, s.sep) + " L", Fmt.liters2(goals.waterMl, s.sep) + " L")
                }
                val accent = Palette.accent(m)
                val track = Palette.trackAlpha
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(6.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) { drawRing(Stats.ratio(value, goal), accent, track) }
                        Text(Fmt.percent(Stats.ratio(value, goal), s.lang), style = NumStyle.Large.copy(fontSize = 20.sp))
                    }
                    Text(s.metric(m), style = LabelStyle.copy(fontSize = 14.sp))
                    Text("$text / $goalText", style = NumStyle.Small)
                }
            }
        }
        if (canOpen) {
            Text(
                s.openDay + " →",
                style = NumStyle.Body.copy(color = Palette.protein, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    }
}

private data class Quad(val a: Float, val b: Float, val c: String, val d: String)

/**
 * Hafta: her gün için yan yana iki ince sütun (protein, lif), hedef = kesikli çizgi.
 * Hedefi aşan sütun en üstte kırpılır; oran yüzdesi altta yazar.
 */
@Composable
private fun WeekBars(st: HistoryUiState, s: AppStrings, onSelect: (Long) -> Unit) {
    val protein = Palette.protein
    val fiber = Palette.fiber
    val water = Palette.water
    val hair = Palette.hairline
    val track = Palette.trackAlpha
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..6) {
            val day = st.from + i
            val t = st.byDay[day] ?: DayTotals()
            val future = day > st.today
            val pr = Stats.ratio(t.proteinG, st.goals.proteinG)
            val fr = Stats.ratio(t.fiberG, st.goals.fiberG)
            val wr = Stats.ratio(t.waterMl.toFloat(), st.goals.waterMl.toFloat())
            Column(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable(enabled = !future, role = Role.Button) { onSelect(day) }
                    .semantics { contentDescription = "${s.weekdaysShort[i]}: ${Fmt.percent(pr, s.lang)} ${s.proteinWord}, ${Fmt.percent(fr, s.lang)} ${s.fiberWord}" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.fillMaxWidth().height(140.dp).padding(horizontal = 4.dp)) {
                    val barW = min(size.width / 4f, 10.dp.toPx())
                    val gap = 3.dp.toPx()
                    val total = barW * 3 + gap * 2
                    val x0 = (size.width - total) / 2f
                    val h = size.height
                    val goalY = h * 0.2f // hedef çizgisi: %100 = yüksekliğin %80'i
                    val bars = listOf(pr to protein, fr to fiber, wr to water)
                    bars.forEachIndexed { k, (r, c) ->
                        val x = x0 + k * (barW + gap)
                        // Gelecek günlerde iz çizilmez: yalnız geçmiş günler görünür.
                        if (future) return@forEachIndexed
                        drawRoundRect(c.copy(alpha = track * 0.7f), Offset(x, goalY), Size(barW, h - goalY), CornerRadius(barW / 2f))
                        if (r > 0f) {
                            val bh = min(r, 1.25f) * (h - goalY)
                            drawRoundRect(c, Offset(x, h - bh), Size(barW, bh), CornerRadius(barW / 2f))
                        }
                    }
                    drawLine(
                        hair, Offset(0f, goalY), Offset(size.width, goalY), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    s.weekdaysShort[i],
                    style = NumStyle.Small.copy(
                        color = if (day == st.today) Palette.textPrimary else Palette.textSecondary,
                        fontWeight = if (day == st.today) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
                Text(LocalDate.ofEpochDay(day).dayOfMonth.toString(), style = NumStyle.Small.copy(color = Palette.textMuted, fontSize = 11.sp))
            }
        }
    }
    Legend(s)
}

@Composable
private fun Legend(s: AppStrings) {
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        for (m in Metric.entries) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.accent(m)))
                Spacer(Modifier.width(6.dp))
                Text(s.metric(m), style = NumStyle.Small)
            }
        }
    }
}

/**
 * Ay: pazartesi başlayan ızgara. Her günde protein (dış) ve lif (iç) için iç içe iki küçük halka;
 * ikisi de tamamsa gün numarası kalın yazılır.
 */
@Composable
private fun MonthGrid(st: HistoryUiState, s: AppStrings, onSelect: (Long) -> Unit) {
    val first = LocalDate.ofEpochDay(st.from)
    val lead = first.dayOfWeek.value - 1
    val days = (st.to - st.from + 1).toInt()
    val cells = lead + days
    val rows = (cells + 6) / 7
    val protein = Palette.protein
    val fiber = Palette.fiber
    val track = Palette.trackAlpha
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            for (w in s.weekdaysShort) {
                Text(w, style = NumStyle.Small.copy(color = Palette.textMuted), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0..6) {
                    val idx = r * 7 + c - lead
                    if (idx < 0 || idx >= days) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = st.from + idx
                        val t = st.byDay[day]
                        val future = day > st.today
                        val pr = t?.let { Stats.ratio(it.proteinG, st.goals.proteinG) } ?: 0f
                        val fr = t?.let { Stats.ratio(it.fiberG, st.goals.fiberG) } ?: 0f
                        val both = pr >= 1f && fr >= 1f
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .clickable(enabled = !future, role = Role.Button) { onSelect(day) }
                                .semantics {
                                    contentDescription = "${idx + 1}: ${Fmt.percent(pr, s.lang)} ${s.proteinWord}, ${Fmt.percent(fr, s.lang)} ${s.fiberWord}"
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!future) {
                                Canvas(Modifier.fillMaxSize().padding(3.dp)) {
                                    drawRing(pr, protein, track, strokeFraction = 0.09f)
                                    inset(size.minDimension * 0.13f) {
                                        drawRing(fr, fiber, track, strokeFraction = 0.1f)
                                    }
                                }
                            }
                            Text(
                                "${idx + 1}",
                                style = NumStyle.Small.copy(
                                    fontSize = 12.sp,
                                    color = when {
                                        future -> Palette.textMuted
                                        day == st.today -> Palette.textPrimary
                                        else -> Palette.textSecondary
                                    },
                                    fontWeight = if (both || day == st.today) FontWeight.Bold else FontWeight.Normal,
                                ),
                            )
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for (m in listOf(Metric.PROTEIN, Metric.FIBER)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.accent(m)))
                    Spacer(Modifier.width(6.dp))
                    Text(s.metric(m), style = NumStyle.Small)
                }
            }
        }
    }
}

/** Dönem özeti: her ölçü için ortalama oran çubuğu + hedefin tutturulduğu gün sayısı. */
@Composable
private fun StatsBlock(stats: PeriodStats, goals: Goals, s: AppStrings) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(s.average, style = LabelStyle)
        StatRow(Metric.PROTEIN, stats.avgProteinRatio, stats.proteinHitDays, stats.days, s)
        StatRow(Metric.FIBER, stats.avgFiberRatio, stats.fiberHitDays, stats.days, s)
        StatRow(Metric.WATER, stats.avgWaterRatio, stats.waterHitDays, stats.days, s)
        Text(s.loggedDays(stats.loggedDays, stats.days), style = NumStyle.Small.copy(color = Palette.textMuted))
    }
}

@Composable
private fun StatRow(m: Metric, ratio: Float, hit: Int, days: Int, s: AppStrings) {
    val accent = Palette.accent(m)
    val track = Palette.trackAlpha
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(s.metric(m), style = NumStyle.Body, modifier = Modifier.weight(1f))
            Text(Fmt.percent(ratio, s.lang), style = NumStyle.Body.copy(color = accent, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val r = CornerRadius(size.height / 2f)
            drawRoundRect(accent.copy(alpha = track), cornerRadius = r)
            val w = size.width * min(ratio, 1f)
            if (w > 0f) drawRoundRect(accent, size = Size(w.coerceAtLeast(size.height), size.height), cornerRadius = r)
        }
        Spacer(Modifier.height(4.dp))
        Text(s.hitDays(hit, days), style = NumStyle.Small)
    }
}
