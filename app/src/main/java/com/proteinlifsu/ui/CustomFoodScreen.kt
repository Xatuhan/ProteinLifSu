package com.proteinlifsu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinlifsu.domain.Fmt
import com.proteinlifsu.domain.Food
import com.proteinlifsu.domain.FoodCatalog
import com.proteinlifsu.domain.Ingredient
import com.proteinlifsu.domain.InputError
import com.proteinlifsu.domain.InputField
import com.proteinlifsu.domain.InputLimits
import com.proteinlifsu.domain.InputValidator
import com.proteinlifsu.domain.Recipe
import com.proteinlifsu.domain.Validated
import com.proteinlifsu.ui.components.ActionButton
import com.proteinlifsu.ui.components.Hairline
import com.proteinlifsu.ui.components.IconAction
import com.proteinlifsu.ui.components.NumberField
import com.proteinlifsu.ui.components.ScreenTopBar
import com.proteinlifsu.ui.components.SectionLabel
import com.proteinlifsu.ui.components.SegmentedPills
import com.proteinlifsu.ui.components.SelectChip
import com.proteinlifsu.ui.components.SoftShape
import com.proteinlifsu.ui.components.softFieldColors
import com.proteinlifsu.ui.i18n.LocalStrings
import com.proteinlifsu.ui.theme.AppIcons
import com.proteinlifsu.ui.theme.NumStyle
import com.proteinlifsu.ui.theme.Palette

private enum class FoodMode { LABEL, RECIPE }

/**
 * Kendi besinini ekleme: paket etiketinden (100 g değerleri) ya da tariften (malzeme + gram;
 * isteğe bağlı pişmiş ağırlık). Sonuç 100 g başına değer olarak saklanır.
 */
@Composable
fun CustomFoodScreen(
    viewModel: TrackerViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    val customFoods by viewModel.customFoods.collectAsStateWithLifecycle()

    var name by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(FoodMode.LABEL) }
    var proteinText by rememberSaveable { mutableStateOf("") }
    var fiberText by rememberSaveable { mutableStateOf("") }
    var servingText by rememberSaveable { mutableStateOf("") }
    var cookedText by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    // Malzemeler "id|gram" dizeleri olarak saklanır (ekran döndürmede kaybolmasın).
    var ingredientKeys by rememberSaveable { mutableStateOf(listOf<String>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var pickedId by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedGrams by rememberSaveable { mutableStateOf("") }

    val customState = rememberUpdatedState(customFoods)
    // Arama sonuçlarındaki besinler (USDA dahil) bellekte yüklüdür; eşzamanlı çözülür.
    fun resolve(id: String?): Food? = viewModel.findLoaded(id) ?: customState.value.firstOrNull { it.id == id }

    val ingredients by remember {
        derivedStateOf {
            ingredientKeys.mapNotNull { key ->
                val (id, g) = key.split('|').let { it.getOrNull(0) to it.getOrNull(1)?.toFloatOrNull() }
                val food = resolve(id)
                if (food != null && g != null) Ingredient(food, g) else null
            }
        }
    }

    val nameV by remember { derivedStateOf { InputValidator.validateName(name) } }
    val proteinV by remember { derivedStateOf { InputValidator.validatePer100(proteinText) } }
    val fiberV by remember { derivedStateOf { InputValidator.validatePer100(fiberText) } }
    val servingV by remember { derivedStateOf { InputValidator.validateOptionalGrams(servingText) } }
    val cookedV by remember { derivedStateOf { Recipe.validateCookedWeight(cookedText) } }
    val recipeV by remember {
        derivedStateOf {
            val cooked = (cookedV as? Validated.Ok)?.value
            Recipe.calculate(ingredients, cooked)
        }
    }

    /** Kaydedilecek (protein, lif) 100 g değerleri ya da null. */
    val per100: Pair<Float, Float>? = when (mode) {
        FoodMode.LABEL -> {
            val p = (proteinV as? Validated.Ok)?.value
            val f = (fiberV as? Validated.Ok)?.value
            if (p != null && f != null && p + f <= 100f && (p > 0f || f > 0f)) p to f else null
        }
        FoodMode.RECIPE -> (recipeV as? Validated.Ok)?.value?.per100?.let { it.proteinG to it.fiberG }
    }
    val canSave = !saving && nameV is Validated.Ok && servingV is Validated.Ok && per100 != null &&
        (mode == FoodMode.LABEL || cookedV is Validated.Ok)

    val save = {
        val values = per100.takeIf { canSave }
        if (values != null) {
            saving = true
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            focus.clearFocus()
            val summary = if (mode == FoodMode.RECIPE) {
                ingredients.joinToString(", ") { "${it.food.name(s.lang)} ${Fmt.grams(it.grams, s.sep)}g" }
            } else {
                null
            }
            viewModel.saveCustomFood(
                name = (nameV as Validated.Ok).value,
                proteinPer100g = values.first,
                fiberPer100g = values.second,
                servingG = (servingV as? Validated.Ok)?.value,
                recipeSummary = summary,
            ) { ok -> saving = false; if (ok) onBack() }
        } else {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            showErrors = true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(
            start = SidePadding,
            end = SidePadding,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "top") { ScreenTopBar(s.newFood, s.back, onBack) }
        item(key = "name") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = InputValidator.sanitizeName(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = NumStyle.Body,
                label = { Text(s.foodName, style = NumStyle.Small) },
                isError = showErrors && nameV is Validated.Invalid,
                supportingText = if (showErrors && nameV is Validated.Invalid) {
                    { Text(s.error(InputError.EMPTY, InputField.NAME), style = NumStyle.Small.copy(color = Palette.danger)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                shape = SoftShape,
                colors = softFieldColors(Palette.protein),
            )
        }
        item(key = "mode") {
            SegmentedPills(
                options = FoodMode.entries,
                selected = mode,
                label = { if (it == FoodMode.LABEL) s.fromLabel else s.fromRecipe },
                accent = Palette.textPrimary,
                onSelect = { mode = it },
                modifier = Modifier.fillMaxWidth(),
                itemWidth = null,
            )
        }
        item(key = "hint") {
            Text(if (mode == FoodMode.LABEL) s.labelHint else s.recipeHint, style = NumStyle.Small.copy(color = Palette.textMuted))
        }

        if (mode == FoodMode.LABEL) {
            item(key = "label-fields") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        value = proteinText, onValueChange = { proteinText = it }, accent = Palette.protein,
                        placeholder = "0", label = s.proteinPer100, modifier = Modifier.weight(1f),
                        errorText = (proteinV as? Validated.Invalid)?.let { s.error(it.error, InputField.PER_100) },
                    )
                    NumberField(
                        value = fiberText, onValueChange = { fiberText = it }, accent = Palette.fiber,
                        placeholder = "0", label = s.fiberPer100, modifier = Modifier.weight(1f),
                        errorText = (fiberV as? Validated.Invalid)?.let { s.error(it.error, InputField.PER_100) },
                    )
                }
                val p = (proteinV as? Validated.Ok)?.value ?: 0f
                val f = (fiberV as? Validated.Ok)?.value ?: 0f
                if (p + f > 100f) Text(s.sumTooLarge, style = NumStyle.Small.copy(color = Palette.danger))
            }
        } else {
            item(key = "ing-title") { SectionLabel(s.ingredients) }
            items(ingredients.size, key = { "ing-$it-${ingredientKeys.getOrNull(it)}" }) { i ->
                val ing = ingredients[i]
                val n = ing.food.nutrientsFor(ing.grams)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ing.food.name(s.lang), style = NumStyle.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${Fmt.grams(ing.grams, s.sep)} g · ${Fmt.grams(n.proteinG, s.sep)} g ${s.proteinShort} · ${Fmt.grams(n.fiberG, s.sep)} g ${s.fiberShort}",
                            style = NumStyle.Small,
                        )
                    }
                    IconAction(AppIcons.Close, s.delete, {
                        ingredientKeys = ingredientKeys.toMutableList().also { it.removeAt(i) }
                    }, tint = Palette.textMuted)
                }
            }
            item(key = "ing-add") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(InputLimits.MAX_LABEL_CHARS) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = NumStyle.Body,
                        placeholder = { Text(s.searchHint, color = Palette.textMuted) },
                        shape = SoftShape,
                        colors = softFieldColors(Palette.protein),
                    )
                    val results = com.proteinlifsu.ui.components.rememberFoodResults(query, customFoods) { viewModel.searchFoods(it) }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results, key = { it.id }) { f ->
                            SelectChip(
                                text = f.name(s.lang),
                                selected = f.id == pickedId,
                                accent = Palette.protein,
                                onClick = {
                                    pickedId = f.id
                                    val g = f.gramsFor(f.availablePortions.first())?.toFloat() ?: f.household.firstOrNull()?.grams ?: 100f
                                    pickedGrams = Fmt.grams(g, ',')
                                },
                                tag = if (f.isCustom) s.yours else null,
                            )
                        }
                    }
                    val picked = resolve(pickedId)
                    if (picked != null) {
                        val gv = InputValidator.validateFoodGrams(pickedGrams, picked.maxGramsPerEntry)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            NumberField(
                                value = pickedGrams, onValueChange = { pickedGrams = it }, accent = Palette.protein,
                                placeholder = s.grams, label = picked.name(s.lang), modifier = Modifier.weight(1f),
                                errorText = (gv as? Validated.Invalid)?.takeIf { it.error == InputError.TOO_LARGE }
                                    ?.let { s.error(it.error, InputField.FOOD_GRAMS, picked.maxGramsPerEntry) },
                            )
                            ActionButton(
                                text = s.add,
                                accent = Palette.protein,
                                enabled = gv is Validated.Ok && ingredientKeys.size < InputLimits.MAX_INGREDIENTS,
                                onClick = {
                                    val g = (gv as? Validated.Ok)?.value ?: return@ActionButton
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                    ingredientKeys = ingredientKeys + "${picked.id}|$g"
                                    pickedId = null
                                    pickedGrams = ""
                                    query = ""
                                },
                                icon = AppIcons.Plus,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
            item(key = "cooked") {
                NumberField(
                    value = cookedText, onValueChange = { cookedText = it }, accent = Palette.protein,
                    placeholder = (recipeV as? Validated.Ok)?.value?.totalGrams?.let { Fmt.grams(it, s.sep) } ?: "g",
                    label = s.cookedWeightOptional, modifier = Modifier.fillMaxWidth(),
                    errorText = (cookedV as? Validated.Invalid)?.let { s.error(it.error, InputField.RECIPE_WEIGHT) },
                )
            }
            item(key = "recipe-result") {
                when (val r = recipeV) {
                    is Validated.Ok -> Column {
                        Text(s.totalWeight(Fmt.grams(r.value.totalGrams, s.sep)), style = NumStyle.Small)
                        Text(
                            s.resultPer100(Fmt.grams(r.value.per100.proteinG, s.sep), Fmt.grams(r.value.per100.fiberG, s.sep)),
                            style = NumStyle.Body.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                    is Validated.Invalid -> Text(
                        when (r.error) {
                            InputError.EMPTY -> s.recipeEmpty
                            InputError.TOO_SMALL -> s.cookedTooSmall
                            else -> s.error(r.error, InputField.RECIPE_WEIGHT)
                        },
                        style = NumStyle.Small.copy(color = if (showErrors || r.error != InputError.EMPTY) Palette.danger else Palette.textMuted),
                    )
                }
            }
        }

        item(key = "sep") { Hairline() }
        item(key = "serving") {
            NumberField(
                value = servingText, onValueChange = { servingText = it }, accent = Palette.protein,
                placeholder = "g", label = s.servingOptional, modifier = Modifier.fillMaxWidth(),
                errorText = (servingV as? Validated.Invalid)?.let { s.error(it.error, InputField.FOOD_GRAMS) },
            )
        }
        item(key = "save") {
            Spacer(Modifier.height(4.dp))
            ActionButton(
                text = s.save,
                accent = Palette.protein,
                // Her zaman dokunulabilir: geçersizse eksik alanlar kırmızıyla gösterilir.
                enabled = !saving,
                onClick = save,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
