package com.proteinlifsu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proteinlifsu.domain.Fmt
import com.proteinlifsu.domain.Goals
import com.proteinlifsu.domain.InputField
import com.proteinlifsu.domain.InputValidator
import com.proteinlifsu.domain.Metric
import com.proteinlifsu.domain.Validated
import com.proteinlifsu.domain.WaterUnit
import com.proteinlifsu.ui.i18n.AppStrings
import com.proteinlifsu.ui.i18n.LocalStrings
import com.proteinlifsu.ui.theme.AppIcons
import com.proteinlifsu.ui.theme.LabelStyle
import com.proteinlifsu.ui.theme.NumStyle
import com.proteinlifsu.ui.theme.Palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** "Bugün" / "Dün" / "28 Eki" + alt satırda tam tarih; dile göre. */
fun dateLabels(epochDay: Long, today: Long, s: AppStrings): Pair<String, String> {
    val date = LocalDate.ofEpochDay(epochDay)
    val title = when (today - epochDay) {
        0L -> s.today
        1L -> s.yesterday
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", s.locale))
    }
    val pattern = if (s.lang == com.proteinlifsu.domain.AppLanguage.TR) "d MMMM EEEE" else "EEEE, MMMM d"
    return title to date.format(DateTimeFormatter.ofPattern(pattern, s.locale))
}

/** [takvim] ‹ Bugün › [ayarlar] — başlığa dokununca bugüne döner; geleceğe gidilemez. */
@Composable
fun DateBar(
    epochDay: Long,
    today: Long,
    /** Arka planda hazırlanmış metinler (bkz. TrackerUiState.dateTitle); burada biçimleme yapılmaz. */
    title: String,
    subtitle: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onCalendar: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val isToday = epochDay >= today
    // Üstte sağa yaslı takvim + ayarlar; altında tarih gezinmesi (dar ekranda tarih sığsın diye iki satır).
    Column(modifier.fillMaxWidth()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconAction(AppIcons.Calendar, s.calendar, onCalendar, tint = Palette.textSecondary)
        IconAction(AppIcons.Sliders, s.settings, onSettings, tint = Palette.textSecondary)
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconAction(AppIcons.ChevronLeft, s.prevDay, { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onPrev() })
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = !isToday, role = Role.Button, onClickLabel = s.backToToday, onClick = onToday),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = NumStyle.Body.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
            Text(
                text = if (isToday) subtitle else "$subtitle · ${s.backToToday}",
                style = NumStyle.Small,
                maxLines = 1,
            )
        }
        IconAction(AppIcons.ChevronRight, s.nextDay, { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onNext() }, enabled = !isToday)
    }
    }
}

/** Gösterge dokunuşunda açılan hedef düzenleme penceresi. */
@Composable
fun GoalDialog(
    metric: Metric,
    goals: Goals,
    onDismiss: () -> Unit,
    onSave: (Metric, Float) -> Unit,
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val accent = Palette.accent(metric)
    val initial = when (metric) {
        Metric.PROTEIN -> Fmt.grams(goals.proteinG, ',')
        Metric.FIBER -> Fmt.grams(goals.fiberG, ',')
        Metric.WATER -> goals.waterMl.toString()
    }
    var text by rememberSaveable(metric) { mutableStateOf(initial) }
    var unit by rememberSaveable(metric) { mutableStateOf(WaterUnit.ML) }
    val field = when (metric) {
        Metric.PROTEIN -> InputField.GOAL_PROTEIN
        Metric.FIBER -> InputField.GOAL_FIBER
        Metric.WATER -> InputField.GOAL_WATER
    }
    val validated by remember(metric) { derivedStateOf { InputValidator.validateGoal(metric, text, unit) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.background,
        shape = RoundedCornerShape(24.dp),
        title = { Text(s.goalTitle(metric), style = LabelStyle.copy(fontSize = 18.sp)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                NumberField(
                    value = text,
                    onValueChange = { text = it },
                    accent = accent,
                    placeholder = if (metric == Metric.WATER) "ml" else s.grams,
                    modifier = Modifier.weight(1f),
                    errorText = (validated as? Validated.Invalid)?.let { s.error(it.error, field) },
                )
                if (metric == Metric.WATER) {
                    SegmentedPills(
                        options = WaterUnit.entries,
                        selected = unit,
                        label = { if (it == WaterUnit.ML) "ml" else "L" },
                        accent = accent,
                        onSelect = { unit = it },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Text("g", style = NumStyle.Body, modifier = Modifier.padding(top = 16.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = validated is Validated.Ok,
                onClick = {
                    val v = validated
                    if (v is Validated.Ok) {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSave(metric, v.value)
                    }
                },
            ) { Text(s.save, color = if (validated is Validated.Ok) accent else Palette.textMuted, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel, color = Palette.textSecondary) }
        },
    )
}
