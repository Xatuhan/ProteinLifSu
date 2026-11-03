package com.proteinlifsu.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proteinlifsu.domain.DayTotals
import com.proteinlifsu.domain.Fmt
import com.proteinlifsu.domain.Goals
import com.proteinlifsu.domain.Metric
import com.proteinlifsu.ui.i18n.LocalStrings
import com.proteinlifsu.ui.theme.LabelStyle
import com.proteinlifsu.ui.theme.NumStyle
import com.proteinlifsu.ui.theme.Palette
import kotlin.math.min

/** Tam halka, saat 12 yönünden başlar. */
private const val START_ANGLE = -90f
private const val SWEEP = 360f

/** Yağ gibi akan, hafif yaylanan geçiş. */
private val gaugeSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow,
)

@Composable
fun GaugePanel(
    epochDay: Long,
    loaded: Boolean,
    totals: DayTotals,
    goals: Goals,
    onGaugeClick: (Metric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val sep = s.sep
    val gramsFmt: (Float) -> String = remember(sep) { { v -> Fmt.grams(v, sep) } }
    val litersFmt: (Float) -> String = remember(sep) { { v -> Fmt.liters2(v.coerceAtLeast(0f).toInt(), sep) } }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricGauge(
            Metric.PROTEIN, totals.proteinG, goals.proteinG, "g", Fmt.grams(goals.proteinG, sep),
            gramsFmt, epochDay, loaded, onGaugeClick, Modifier.weight(1f),
        )
        MetricGauge(
            Metric.FIBER, totals.fiberG, goals.fiberG, "g", Fmt.grams(goals.fiberG, sep),
            gramsFmt, epochDay, loaded, onGaugeClick, Modifier.weight(1f),
        )
        MetricGauge(
            Metric.WATER, totals.waterMl.toFloat(), goals.waterMl.toFloat(), "L", Fmt.liters2(goals.waterMl, sep),
            litersFmt, epochDay, loaded, onGaugeClick, Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricGauge(
    metric: Metric,
    consumed: Float,
    goal: Float,
    unitText: String,
    goalText: String,
    format: (Float) -> String,
    epochDay: Long,
    loaded: Boolean,
    onClick: (Metric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val accent = Palette.accent(metric)
    val trackAlpha = Palette.trackAlpha
    val title = s.metric(metric)
    val animated: State<Float> = animateFloatAsState(
        targetValue = consumed,
        animationSpec = gaugeSpring,
        label = "gauge-$metric",
    )
    // Rakam metni yalnızca biçimlenmiş dize değiştiğinde yeniden oluşturulur.
    val valueText by remember(format) { derivedStateOf { format(animated.value) } }
    val lang = s.lang
    val percentText by remember(goal, lang) {
        derivedStateOf { if (goal > 0f) Fmt.percent(animated.value / goal, lang) else Fmt.percent(0f, lang) }
    }

    // Hedefe ulaşıldığı an (yalnızca geçişte, gün değişiminde değil) tok bir titreşim.
    val haptics = LocalHapticFeedback.current
    val reached = goal > 0f && consumed >= goal
    var lastReached by remember(epochDay) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(reached, epochDay, loaded) {
        if (!loaded) return@LaunchedEffect
        if (lastReached == false && reached) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        lastReached = reached
    }

    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClickLabel = s.editGoal) { onClick(metric) }
            .semantics { contentDescription = "$title: $valueText / $goalText $unitText. ${s.editGoal}" }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Animasyonlu değer yalnızca çizim aşamasında okunur: kareler yeniden oluşturma
            // (recomposition) tetiklemez, sadece yeniden çizer.
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRing(
                    progress = if (goal > 0f) animated.value / goal else 0f,
                    accent = accent,
                    trackAlpha = trackAlpha,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = valueText, style = NumStyle.Large, maxLines = 1)
                Text(text = "/ $goalText $unitText", style = NumStyle.Small, maxLines = 1)
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = LabelStyle.copy(fontSize = 14.sp))
            Text(text = "  $percentText", style = NumStyle.Small.copy(color = accent))
        }
    }
}

/** Yumuşak halka: soluk iz + yuvarlak uçlu dolum; hedef aşıldıysa içte ince ikinci tur. */
fun DrawScope.drawRing(progress: Float, accent: Color, trackAlpha: Float, strokeFraction: Float = 0.085f) {
    val s = min(size.width, size.height)
    val c = Offset(size.width / 2f, size.height / 2f)
    val stroke = s * strokeFraction
    val radius = s / 2f - stroke / 2f - 1.dp.toPx()
    val topLeft = Offset(c.x - radius, c.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    val p = if (progress.isFinite()) progress.coerceAtLeast(0f) else 0f
    val filled = min(p, 1f)

    drawCircle(color = accent.copy(alpha = trackAlpha), radius = radius, center = c, style = Stroke(width = stroke))
    if (filled <= 0.001f) return

    drawArc(
        color = accent,
        startAngle = START_ANGLE,
        sweepAngle = SWEEP * filled,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )

    if (p > 1f) {
        val over = min(p - 1f, 1f)
        val r2 = radius - stroke * 1.15f
        if (r2 > 0f) {
            drawArc(
                color = accent.copy(alpha = 0.5f),
                startAngle = START_ANGLE, sweepAngle = SWEEP * over, useCenter = false,
                topLeft = Offset(c.x - r2, c.y - r2), size = Size(r2 * 2f, r2 * 2f),
                style = Stroke(width = stroke * 0.35f, cap = StrokeCap.Round),
            )
        }
    }
}
