package com.proteinlifsu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.proteinlifsu.domain.EntryKind
import com.proteinlifsu.domain.Fmt
import com.proteinlifsu.domain.Food
import com.proteinlifsu.domain.FoodCatalog
import com.proteinlifsu.domain.LogEntry
import com.proteinlifsu.domain.PortionType
import com.proteinlifsu.ui.i18n.AppStrings
import com.proteinlifsu.ui.i18n.LocalStrings
import com.proteinlifsu.ui.theme.AppIcons
import com.proteinlifsu.ui.theme.NumStyle
import com.proteinlifsu.ui.theme.Palette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Kaydın görünen adı: yerleşik besinse seçili dildeki ad, değilse kaydedildiği andaki ad. */
fun displayName(e: LogEntry, s: AppStrings, find: (String?) -> Food? = FoodCatalog::find): String = when (e.kind) {
    EntryKind.WATER -> translateWaterLabel(e.label, s)
    EntryKind.FOOD -> find(e.foodId)?.takeIf { !it.isCustom }?.name(s.lang) ?: e.label
}

/** Su kaydı hangi dilde kaydedildiyse edilsin, bilinen ölçü adları seçili dile çevrilir. */
private fun translateWaterLabel(label: String, s: AppStrings): String {
    for (k in com.proteinlifsu.domain.WaterPresetKind.entries) {
        if (label == com.proteinlifsu.ui.i18n.TrStrings.waterPreset(k) || label == com.proteinlifsu.ui.i18n.EnStrings.waterPreset(k)) {
            return s.waterPreset(k)
        }
    }
    if (label == com.proteinlifsu.ui.i18n.TrStrings.waterEntryLabel || label == com.proteinlifsu.ui.i18n.EnStrings.waterEntryLabel) {
        return s.waterEntryLabel
    }
    return label
}

private fun detailText(e: LogEntry, s: AppStrings, food: Food?): String = when (e.kind) {
    EntryKind.WATER -> Fmt.water(e.waterMl, s.sep)
    EntryKind.FOOD -> {
        val portionText = when (val p = e.portion) {
            null, PortionType.GRAM -> null
            PortionType.HOUSEHOLD -> e.portionLabel
            PortionType.PIECE -> s.piece(food?.piece ?: com.proteinlifsu.domain.PieceUnit.SERVING)
            else -> s.portion(p)
        }
        val gramsText = "${Fmt.grams(e.grams, s.sep)} g"
        if (portionText != null) "$portionText · ~$gramsText" else gramsText
    }
}

/** Günlük akış satırı: sola kaydırınca ya da × ile tek dokunuşta silinir. */
@Composable
fun LogEntryRow(
    entry: LogEntry,
    onDelete: (LogEntry) -> Unit,
    modifier: Modifier = Modifier,
    find: (String?) -> Food? = FoodCatalog::find,
    zone: ZoneId = ZoneId.systemDefault(),
    nested: Boolean = false,
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState()
    val food = remember(entry.foodId, find) { find(entry.foodId) }
    val name = displayName(entry, s) { food }
    val time = remember(entry.createdAt, zone) { Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalTime().format(timeFormat) }
    val detail = remember(entry, s, food) { detailText(entry, s, food) }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onDelete(entry)
            }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Palette.danger.copy(alpha = 0.12f))
                    .padding(end = 18.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(AppIcons.Trash, contentDescription = null, tint = Palette.danger, modifier = Modifier.size(20.dp))
            }
        },
    ) {
        val line = Palette.hairline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Kaydırırken alttaki kırmızı görünmesin diye sayfa zemini (renkli fon değil).
                .background(Palette.background)
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    if (nested) {
                        // Grubun içindeki kayıtlar: solda ince dikey kılavuz çizgisi.
                        val x = 6.dp.toPx()
                        drawLine(line, Offset(x, 0f), Offset(x, size.height), stroke)
                    } else {
                        // Satırları ayıran ince alt çizgi (yazıyla aynı hizadan başlar).
                        val y = size.height - stroke / 2
                        drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
                    }
                }
                .padding(start = if (nested) 20.dp else 0.dp, top = if (nested) 8.dp else 14.dp, bottom = if (nested) 8.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(time, style = NumStyle.Small.copy(color = if (nested) Palette.textSecondary else Palette.textMuted))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // Ayrıntı satırında ad grup başlığında yazdığı için yalnız ölçü/miktar gösterilir (su ölçüsünün adı hariç).
                if (!nested || entry.kind == EntryKind.WATER) {
                    Text(name, style = if (nested) NumStyle.Small.copy(color = Palette.textPrimary) else NumStyle.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Su ayrıntısında miktar sağda zaten yazar; alt satır tekrar olmasın.
                if (!(nested && entry.kind == EntryKind.WATER)) {
                    Text(detail, style = NumStyle.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                when (entry.kind) {
                    EntryKind.WATER -> Text(
                        "+${Fmt.water(entry.waterMl, s.sep)}",
                        style = NumStyle.Body.copy(color = Palette.water, fontWeight = FontWeight.Bold),
                    )
                    EntryKind.FOOD -> {
                        Text("+${Fmt.grams(entry.proteinG, s.sep)} g ${s.proteinShort}", style = NumStyle.Small.copy(color = Palette.protein, fontWeight = FontWeight.SemiBold))
                        Text("+${Fmt.grams(entry.fiberG, s.sep)} g ${s.fiberShort}", style = NumStyle.Small.copy(color = Palette.fiber, fontWeight = FontWeight.SemiBold))
                        if (entry.waterMl > 0) {
                            Text(s.waterFromFood(entry.waterMl), style = NumStyle.Small.copy(color = Palette.water, fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(role = Role.Button, onClickLabel = s.delete) {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDelete(entry)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Close, contentDescription = s.deleteEntry(name), tint = Palette.textMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Birleşik grup satırı: "Su · 3 kez · son 18:42  +700 ml". Dokununca ayrıntılar (her kayıt,
 * saati ve miktarı) açılır/kapanır; silme ayrıntı satırlarından yapılır.
 */
@Composable
fun LogGroupRow(
    group: com.proteinlifsu.domain.LogGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    find: (String?) -> Food? = FoodCatalog::find,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val first = group.first
    val food = remember(first.foodId, find) { find(first.foodId) }
    val name = if (group.kind == EntryKind.WATER) s.waterEntryLabel else displayName(first, s) { food }
    val last = remember(first.createdAt, zone) { Instant.ofEpochMilli(first.createdAt).atZone(zone).toLocalTime().format(timeFormat) }
    val line = Palette.hairline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Açıkken alt çizgi yerine ayrıntılar başlar; kapalıyken satır ayırıcı.
                if (!expanded) {
                    val stroke = 1.dp.toPx()
                    val y = size.height - stroke / 2
                    drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
                }
            }
            .clickable(role = Role.Button, onClickLabel = if (expanded) s.hideDetails else s.showDetails) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onToggle()
            }
            .padding(top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = NumStyle.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.groupSummary(group.count, last), style = NumStyle.Small, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            when (group.kind) {
                EntryKind.WATER -> Text(
                    "+${Fmt.water(group.waterMl, s.sep)}",
                    style = NumStyle.Body.copy(color = Palette.water, fontWeight = FontWeight.Bold),
                )
                EntryKind.FOOD -> {
                    Text("+${Fmt.grams(group.proteinG, s.sep)} g ${s.proteinShort}", style = NumStyle.Small.copy(color = Palette.protein, fontWeight = FontWeight.SemiBold))
                    Text("+${Fmt.grams(group.fiberG, s.sep)} g ${s.fiberShort}", style = NumStyle.Small.copy(color = Palette.fiber, fontWeight = FontWeight.SemiBold))
                    if (group.waterMl > 0) {
                        Text(s.waterFromFood(group.waterMl), style = NumStyle.Small.copy(color = Palette.water, fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = Palette.textMuted,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = if (expanded) 90f else 0f },
            )
        }
    }
}

@Composable
fun LogHeader(count: Int, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    SectionLabel(text = s.log, trailing = if (count == 0) null else s.logCount(count), modifier = modifier)
}

@Composable
fun EmptyLog(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Column(modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Text(s.emptyLog, style = NumStyle.Small)
        Text(s.emptyLogHint, style = NumStyle.Small.copy(color = Palette.textMuted))
    }
}
