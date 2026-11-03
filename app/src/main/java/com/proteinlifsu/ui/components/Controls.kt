package com.proteinlifsu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proteinlifsu.domain.InputValidator
import com.proteinlifsu.ui.theme.LabelStyle
import com.proteinlifsu.ui.theme.NumStyle
import com.proteinlifsu.ui.theme.Palette

/** Alanlar ve satırlar için yumuşak köşe. */
val SoftShape = RoundedCornerShape(14.dp)

/** Çip ve düğmeler için hap biçimi. */
val PillShape = RoundedCornerShape(50)

/** Bölüm başlığı + sağda isteğe bağlı küçük not. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = LabelStyle, modifier = Modifier.weight(1f))
        if (trailing != null) Text(text = trailing, style = NumStyle.Small.copy(color = Palette.textMuted))
    }
}

/** Tam genişlik ince ayraç. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Palette.hairline),
    )
}

@Composable
fun softFieldColors(accent: Color): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = Color.Transparent,
    errorBorderColor = Palette.danger,
    cursorColor = accent,
    focusedContainerColor = Palette.background,
    unfocusedContainerColor = Palette.surface,
    errorContainerColor = Palette.surface,
    focusedTextColor = Palette.textPrimary,
    unfocusedTextColor = Palette.textPrimary,
)

/**
 * Sayısal giriş kutusu. Her tuş vuruşu [InputValidator.sanitizeDecimalInput]'tan geçer:
 * eksi, harf, üs gösterimi ya da ikinci ayırıcı yazılamaz; uzunluk sınırlıdır.
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    label: String? = null,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(InputValidator.sanitizeDecimalInput(it)) },
        modifier = modifier,
        singleLine = true,
        textStyle = NumStyle.Body,
        label = label?.let { { Text(it, style = NumStyle.Small) } },
        placeholder = { Text(placeholder, style = NumStyle.Body.copy(color = Palette.textMuted)) },
        isError = errorText != null,
        supportingText = errorText?.let { msg ->
            { Text(msg, style = NumStyle.Small.copy(color = Palette.danger)) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = SoftShape,
        colors = softFieldColors(accent),
    )
}

/** Hap biçimli seçim segmenti (ör. ml | L, Gün | Hafta | Ay). */
@Composable
fun <T> SegmentedPills(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    accent: Color,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemWidth: androidx.compose.ui.unit.Dp? = 44.dp,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(Palette.surface)
            .padding(4.dp),
    ) {
        for (o in options) {
            val isSel = o == selected
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .then(if (itemWidth != null) Modifier.width(itemWidth) else Modifier.weight(1f))
                    .clip(PillShape)
                    .background(if (isSel) Palette.background else Color.Transparent)
                    .semantics { this.selected = isSel }
                    .clickable(role = Role.Tab) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o),
                    style = NumStyle.Body.copy(
                        color = if (isSel) accent else Palette.textMuted,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Seçim çipi: hap biçimli, yumuşak dolgu; seçiliyken vurgu renginin açık tonu. */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) accent.copy(alpha = 0.18f) else Palette.surface)
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = NumStyle.Body.copy(
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Palette.textPrimary else Palette.textSecondary,
            ),
            maxLines = 1,
        )
        if (tag != null) {
            Spacer(Modifier.width(6.dp))
            Text(tag, style = NumStyle.Small.copy(fontSize = 11.sp, color = accent, fontWeight = FontWeight.SemiBold))
        }
    }
}

/** Ana eylem düğmesi: hap biçimli vurgu dolgusu. */
@Composable
fun ActionButton(
    text: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val fg = if (enabled) Palette.onAccent else Palette.textMuted
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(PillShape)
            .background(if (enabled) accent else Palette.surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = NumStyle.Body.copy(color = fg, fontWeight = FontWeight.SemiBold))
    }
}

/** 48dp dokunma alanlı ikon düğmesi. */
@Composable
fun IconAction(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true, tint: Color? = null) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(PillShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint ?: if (enabled) Palette.textPrimary else Palette.hairline,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Ekran başlığı: geri + başlık. */
@Composable
fun ScreenTopBar(title: String, backLabel: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconAction(com.proteinlifsu.ui.theme.AppIcons.ChevronLeft, backLabel, onBack)
        Text(title, style = LabelStyle.copy(fontSize = 20.sp), modifier = Modifier.padding(start = 4.dp))
    }
}
