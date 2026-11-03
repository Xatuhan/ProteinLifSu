package com.proteinlifsu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proteinlifsu.domain.Fmt
import com.proteinlifsu.domain.Food
import com.proteinlifsu.domain.InputError
import com.proteinlifsu.domain.InputField
import com.proteinlifsu.domain.InputLimits
import com.proteinlifsu.domain.InputValidator
import com.proteinlifsu.domain.Metric
import com.proteinlifsu.domain.PortionType
import com.proteinlifsu.domain.Validated
import com.proteinlifsu.domain.WaterPreset
import com.proteinlifsu.domain.WaterPresets
import com.proteinlifsu.domain.WaterUnit
import com.proteinlifsu.ui.i18n.AppStrings
import com.proteinlifsu.ui.i18n.LocalStrings
import com.proteinlifsu.ui.theme.AppIcons
import com.proteinlifsu.ui.theme.LabelStyle
import com.proteinlifsu.ui.theme.NumStyle
import com.proteinlifsu.ui.theme.Palette

// ---------------------------------------------------------------- SU

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterSection(
    onAddWater: (label: String, ml: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    var text by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(WaterUnit.ML) }
    var showError by rememberSaveable { mutableStateOf(false) }

    val validated by remember { derivedStateOf { InputValidator.validateWater(text, unit) } }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(s.metric(Metric.WATER))
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (preset in WaterPresets.all) {
                key(preset.kind) {
                    val label = s.waterPreset(preset.kind)
                    WaterPresetChip(preset, label, s) {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onAddWater(label, preset.ml)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val submit = {
            val v = validated
            if (v is Validated.Ok) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onAddWater(s.waterEntryLabel, v.value)
                text = ""
                showError = false
                focus.clearFocus()
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                showError = true
            }
        }
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                value = text,
                onValueChange = { text = it; showError = false },
                accent = Palette.water,
                placeholder = if (unit == WaterUnit.ML) s.waterPlaceholderMl else s.waterPlaceholderL,
                modifier = Modifier.weight(1f),
                errorText = (validated as? Validated.Invalid)
                    ?.takeIf { showError || (text.isNotEmpty() && it.error != InputError.EMPTY && it.error != InputError.NOT_A_NUMBER) }
                    ?.let { s.error(it.error, InputField.WATER) },
                onDone = submit,
            )
            SegmentedPills(
                options = WaterUnit.entries,
                selected = unit,
                label = { if (it == WaterUnit.ML) "ml" else "L" },
                accent = Palette.water,
                onSelect = { unit = it },
                modifier = Modifier.padding(top = 4.dp),
            )
            ActionButton(
                text = s.add,
                accent = Palette.water,
                enabled = validated is Validated.Ok,
                onClick = submit,
                icon = AppIcons.Plus,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Zeminsiz su ölçüsü: yalnız canlı mavi miktar + altında sade etiket. */
@Composable
private fun WaterPresetChip(preset: WaterPreset, label: String, s: AppStrings, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(SoftShape)
            .clickable(role = Role.Button, onClickLabel = s.addPreset(label), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Drop, contentDescription = null, tint = Palette.water, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                Fmt.water(preset.ml, s.sep),
                style = NumStyle.Body.copy(color = Palette.water, fontWeight = FontWeight.Bold, fontSize = 16.sp),
            )
        }
        Text(label, style = NumStyle.Small)
    }
}

// ---------------------------------------------------------------- BESİN

/** Seçili ölçü anahtarı: "E:<PortionType>", "H:<sıra>" (USDA ev ölçüsü) ya da "G" (gram). */
private const val GRAM_KEY = "G"

private fun gramsForKey(food: Food, key: String): Float? = when {
    key.startsWith("E:") -> PortionType.entries.firstOrNull { it.name == key.substring(2) }?.let { food.gramsFor(it)?.toFloat() }
    key.startsWith("H:") -> key.substring(2).toIntOrNull()?.let { food.household.getOrNull(it)?.grams }
    else -> null
}

private fun defaultKey(food: Food): String {
    val first = food.availablePortions.firstOrNull { it != PortionType.GRAM }
    return when {
        first != null -> "E:${first.name}"
        food.household.isNotEmpty() -> "H:0"
        else -> GRAM_KEY
    }
}

/** Arama sonuçları: sorgu değişince kısa bir beklemeden sonra arka planda aranır. */
@Composable
fun rememberFoodResults(query: String, refreshKey: Any?, search: suspend (String) -> List<Food>): List<Food> {
    val results by produceState(initialValue = emptyList<Food>(), query, refreshKey) {
        if (query.isNotBlank()) kotlinx.coroutines.delay(150)
        value = search(query)
    }
    return results
}

@Composable
fun FoodSection(
    customFoods: List<Food>,
    search: suspend (String) -> List<Food>,
    resolve: suspend (String) -> Food?,
    onAddFood: (food: Food, portion: PortionType, grams: Float, portionLabel: String?) -> Unit,
    onCreateFood: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current

    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var portionKey by rememberSaveable { mutableStateOf(GRAM_KEY) }
    var gramsText by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Food?>(null) }

    val results = rememberFoodResults(query, customFoods, search)
    var listOpen by remember(query) { mutableStateOf(true) }
    var shown by remember(query) { mutableStateOf(6) }

    // Ekran döndürmede seçili besin kimlikten yeniden çözülür.
    LaunchedEffect(selectedId, customFoods) {
        val id = selectedId
        if (id == null) selected = null else if (selected?.id != id) selected = resolve(id)
    }

    val maxGrams = selected?.maxGramsPerEntry ?: InputLimits.MAX_FOOD_GRAMS
    val validated by remember(maxGrams) { derivedStateOf { InputValidator.validateFoodGrams(gramsText, maxGrams) } }

    val selectFood: (Food) -> Unit = remember {
        { food ->
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            selected = food
            selectedId = food.id
            val key = defaultKey(food)
            portionKey = key
            gramsText = gramsForKey(food, key)?.let { Fmt.grams(it, ',') } ?: "100"
            showError = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(s.food)
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(InputLimits.MAX_LABEL_CHARS) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = NumStyle.Body,
            leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = Palette.textMuted) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    Icon(
                        AppIcons.Close,
                        contentDescription = s.clearSearch,
                        tint = Palette.textMuted,
                        modifier = Modifier.clickable { query = "" }.padding(8.dp),
                    )
                }
            } else {
                null
            },
            placeholder = { Text(s.searchHint, color = Palette.textMuted) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = SoftShape,
            colors = softFieldColors(Palette.protein),
        )
        Spacer(Modifier.height(10.dp))

        if (query.isBlank()) {
            // Boş aramada: kendi besinlerin + elle seçilmiş sık besinler (yatay, hızlı seçim).
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp),
            ) {
                item(key = "create", contentType = "create") {
                    SelectChip(text = s.addOwnFood, selected = false, accent = Palette.protein, onClick = onCreateFood)
                }
                items(results, key = { it.id }, contentType = { "food" }) { food ->
                    SelectChip(
                        text = food.name(s.lang),
                        selected = food.id == selectedId,
                        accent = Palette.protein,
                        onClick = { selectFood(food) },
                        tag = if (food.isCustom) s.yours else null,
                    )
                }
            }
        } else if (listOpen) {
            // Aramada: uzun USDA adları okunsun diye dikey liste.
            val visible = results.take(shown)
            for (food in visible) {
                key(food.id) {
                    FoodResultRow(food, food.id == selectedId) {
                        selectFood(food)
                        listOpen = false
                    }
                }
            }
            if (results.size > shown) {
                Text(
                    s.showMore(results.size - shown),
                    style = NumStyle.Body.copy(color = Palette.protein, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .clip(SoftShape)
                        .clickable { shown += 10 }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                )
            }
            if (results.isEmpty()) {
                Text(s.noMatch, style = NumStyle.Small, modifier = Modifier.padding(top = 4.dp))
            }
            Text(
                s.addOwnFood,
                style = NumStyle.Body.copy(color = Palette.textSecondary),
                modifier = Modifier
                    .clip(SoftShape)
                    .clickable(onClick = onCreateFood)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
            )
        } else {
            Text(
                s.showResults(results.size),
                style = NumStyle.Small.copy(color = Palette.protein, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(SoftShape)
                    .clickable { listOpen = true }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            )
        }

        val food = selected
        if (food != null) {
            Spacer(Modifier.height(16.dp))
            Text(food.name(s.lang), style = LabelStyle)
            if (food.autoTranslated && s.lang == com.proteinlifsu.domain.AppLanguage.TR) {
                Text(food.nameEn, style = NumStyle.Small.copy(color = Palette.textMuted, fontSize = 12.sp))
            }
            Text(
                text = (if (food.fiberKnown) {
                    s.per100(Fmt.grams(food.proteinPer100g, s.sep), Fmt.grams(food.fiberPer100g, s.sep))
                } else {
                    s.per100NoFiber(Fmt.grams(food.proteinPer100g, s.sep))
                }) + (food.waterPer100g?.let { " · " + s.waterPercent(Fmt.grams(it, s.sep)) } ?: ""),
                style = NumStyle.Small,
            )
            Spacer(Modifier.height(10.dp))

            // Birleşik "Kase / Tabak / Porsiyon / USDA ölçüsü / Gram" segmenti.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val chips = buildList {
                    for (p in food.availablePortions) {
                        if (p == PortionType.GRAM) continue
                        val name = if (p == PortionType.PIECE) s.piece(food.piece) else s.portion(p)
                        add(Triple("E:${p.name}", name, food.gramsFor(p)?.toFloat()))
                    }
                    food.household.forEachIndexed { i, h -> add(Triple("H:$i", h.label(s.lang), h.grams)) }
                    add(Triple(GRAM_KEY, s.portion(PortionType.GRAM), null))
                }
                for ((chipKey, name, grams) in chips) {
                    key(chipKey) {
                        SelectChip(
                            text = if (grams != null) "$name · ${Fmt.grams(grams, s.sep)}g" else name,
                            selected = chipKey == portionKey,
                            accent = Palette.protein,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                portionKey = chipKey
                                if (grams != null) gramsText = Fmt.grams(grams, ',')
                                showError = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            val submit = {
                val v = validated
                if (v is Validated.Ok) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    val portion: PortionType
                    var label: String? = null
                    when {
                        portionKey.startsWith("E:") ->
                            portion = PortionType.entries.firstOrNull { it.name == portionKey.substring(2) } ?: PortionType.GRAM
                        portionKey.startsWith("H:") -> {
                            portion = PortionType.HOUSEHOLD
                            label = portionKey.substring(2).toIntOrNull()?.let { food.household.getOrNull(it)?.label(s.lang) }
                        }
                        else -> portion = PortionType.GRAM
                    }
                    onAddFood(food, portion, v.value, label)
                    showError = false
                    focus.clearFocus()
                } else {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    showError = true
                }
            }

            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = gramsText,
                    onValueChange = {
                        gramsText = it
                        // Kullanıcı kendi gramını yazdıysa ölçü "Gram"a geçer.
                        val g = gramsForKey(food, portionKey)
                        if (g == null || Fmt.grams(g, ',') != it) portionKey = GRAM_KEY
                        showError = false
                    },
                    accent = Palette.protein,
                    placeholder = s.grams,
                    modifier = Modifier.weight(1f),
                    errorText = (validated as? Validated.Invalid)
                        ?.takeIf { showError || it.error == InputError.TOO_LARGE }
                        ?.let { s.error(it.error, InputField.FOOD_GRAMS, food.maxGramsPerEntry) },
                    onDone = submit,
                )
                ActionButton(
                    text = s.add,
                    accent = Palette.protein,
                    enabled = validated is Validated.Ok,
                    onClick = submit,
                    icon = AppIcons.Plus,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // Şeffaf karşılık: (~220g → 32g Protein, 4g Lif)
            val preview by remember(food) {
                derivedStateOf { (validated as? Validated.Ok)?.value?.let { g -> g to food.nutrientsFor(g) } }
            }
            val pv = preview
            val proteinColor = Palette.protein
            val fiberColor = Palette.fiber
            val waterColor = Palette.water
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (pv == null) {
                    buildAnnotatedString { append(s.previewEmpty) }
                } else {
                    buildAnnotatedString {
                        append("(~${Fmt.grams(pv.first, s.sep)}g → ")
                        withStyle(SpanStyle(color = proteinColor, fontWeight = FontWeight.SemiBold)) {
                            append("${Fmt.grams(pv.second.proteinG, s.sep)}g ${s.proteinWord}")
                        }
                        append(", ")
                        if (food.fiberKnown) {
                            withStyle(SpanStyle(color = fiberColor, fontWeight = FontWeight.SemiBold)) {
                                append("${Fmt.grams(pv.second.fiberG, s.sep)}g ${s.fiberWord}")
                            }
                        } else {
                            append(s.fiberUnknown)
                        }
                        val ml = food.waterMlFor(pv.first)
                        if (ml > 0) {
                            append(", ")
                            withStyle(SpanStyle(color = waterColor, fontWeight = FontWeight.SemiBold)) {
                                append("$ml ml ${s.waterWord}")
                            }
                        }
                        append(")")
                    }
                },
                style = NumStyle.Body.copy(color = Palette.textSecondary),
            )
        }
    }
}

/** Arama sonucu satırı: ad (2 satıra kadar), Türkçe modda İngilizce asıl ad, 100 g değerleri. */
@Composable
private fun FoodResultRow(food: Food, selected: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SoftShape)
            .background(if (selected) Palette.protein.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                food.name(s.lang),
                style = NumStyle.Body,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (food.isCustom) {
                Spacer(Modifier.width(6.dp))
                Text(s.yours, style = NumStyle.Small.copy(fontSize = 11.sp, color = Palette.protein, fontWeight = FontWeight.SemiBold))
            }
        }
        if (food.autoTranslated && s.lang == com.proteinlifsu.domain.AppLanguage.TR) {
            Text(
                food.nameEn,
                style = NumStyle.Small.copy(color = Palette.textMuted, fontSize = 12.sp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Text(
            if (food.fiberKnown) {
                s.per100(Fmt.grams(food.proteinPer100g, s.sep), Fmt.grams(food.fiberPer100g, s.sep))
            } else {
                s.per100NoFiber(Fmt.grams(food.proteinPer100g, s.sep))
            },
            style = NumStyle.Small.copy(fontSize = 12.sp),
        )
    }
}
