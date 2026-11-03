package com.proteinlifsu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinlifsu.domain.Food
import com.proteinlifsu.domain.LogEntry
import com.proteinlifsu.domain.Metric
import com.proteinlifsu.domain.PortionType
import com.proteinlifsu.ui.components.DateBar
import com.proteinlifsu.ui.components.EmptyLog
import com.proteinlifsu.ui.components.FoodSection
import com.proteinlifsu.ui.components.GaugePanel
import com.proteinlifsu.ui.components.GoalDialog
import com.proteinlifsu.ui.components.Hairline
import com.proteinlifsu.ui.components.LogEntryRow
import com.proteinlifsu.ui.components.LogGroupRow
import com.proteinlifsu.ui.components.LogHeader
import com.proteinlifsu.ui.components.WaterSection
import com.proteinlifsu.ui.components.displayName
import com.proteinlifsu.ui.i18n.AppStrings
import com.proteinlifsu.ui.i18n.LocalStrings

val SidePadding = 16.dp

@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel,
    contentPadding: PaddingValues,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateFood: () -> Unit,
) {
    val s = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val customFoods by viewModel.customFoods.collectAsStateWithLifecycle()
    var editingGoal by rememberSaveable { mutableStateOf<Metric?>(null) }
    // Açık gruplar (ekran döndürmede korunur).
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }

    // Lambda'lar bir kez oluşturulur: alt bileşenlere her seferinde yeni örnek gitmez.
    val onPrev = remember(viewModel) { { viewModel.previousDay() } }
    val onNext = remember(viewModel) { { viewModel.nextDay() } }
    val onToday = remember(viewModel) { { viewModel.goToday() } }
    val onGaugeClick = remember { { m: Metric -> editingGoal = m } }
    val onAddWater = remember(viewModel) { { label: String, ml: Int -> viewModel.addWater(label, ml) } }
    val onAddFood = remember(viewModel) {
        { food: Food, portion: PortionType, grams: Float, label: String? -> viewModel.addFood(food, portion, grams, label) }
    }
    val search: suspend (String) -> List<Food> = remember(viewModel) { { q -> viewModel.searchFoods(q) } }
    val resolve: suspend (String) -> Food? = remember(viewModel) { { id -> viewModel.resolveFood(id) } }
    val find: (String?) -> Food? = remember(viewModel) { { id -> viewModel.findLoaded(id) } }
    val onDelete = remember(viewModel, s) { { e: LogEntry -> viewModel.delete(e, displayName(e, s, find)) } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = SidePadding,
            end = SidePadding,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "date", contentType = "date") {
            DateBar(
                epochDay = state.epochDay,
                today = state.today,
                title = state.dateTitle,
                subtitle = state.dateSubtitle,
                onPrev = onPrev,
                onNext = onNext,
                onToday = onToday,
                onCalendar = onOpenCalendar,
                onSettings = onOpenSettings,
            )
        }
        item(key = "gauges", contentType = "gauges") {
            GaugePanel(
                epochDay = state.epochDay,
                loaded = state.loaded,
                totals = state.totals,
                goals = state.goals,
                onGaugeClick = onGaugeClick,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
        }
        item(key = "sep1", contentType = "sep") { Hairline() }
        item(key = "water", contentType = "water") {
            WaterSection(onAddWater = onAddWater, modifier = Modifier.padding(vertical = 16.dp))
        }
        item(key = "sep2", contentType = "sep") { Hairline() }
        item(key = "food", contentType = "food") {
            FoodSection(
                customFoods = customFoods,
                search = search,
                resolve = resolve,
                onAddFood = onAddFood,
                onCreateFood = onCreateFood,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        item(key = "sep3", contentType = "sep") { Hairline() }
        item(key = "log-header", contentType = "log-header") {
            LogHeader(count = state.entries.size, modifier = Modifier.padding(top = 16.dp, bottom = 10.dp))
        }
        if (state.loaded && state.entries.isEmpty()) {
            item(key = "empty", contentType = "empty") { EmptyLog() }
        }
        for (group in state.groups) {
            if (group.count == 1) {
                // Tek kayıt: eskisi gibi düz satır (kaydırarak ya da × ile silinir).
                val entry = group.first
                item(key = entry.id, contentType = "entry") {
                    LogEntryRow(
                        entry = entry,
                        onDelete = onDelete,
                        find = find,
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                val isOpen = group.key in expanded
                item(key = "g:" + group.key, contentType = "group") {
                    LogGroupRow(
                        group = group,
                        expanded = isOpen,
                        onToggle = { expanded = if (isOpen) expanded - group.key else expanded + group.key },
                        find = find,
                        modifier = Modifier.animateItem(),
                    )
                }
                if (isOpen) {
                    items(group.entries, key = { it.id }, contentType = { "entry-nested" }) { entry ->
                        LogEntryRow(
                            entry = entry,
                            onDelete = onDelete,
                            find = find,
                            nested = true,
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "g-end:" + group.key, contentType = "sep") {
                        // Açık grubun bittiğini gösteren ayırıcı.
                        Hairline(Modifier.padding(top = 6.dp).animateItem())
                    }
                }
            }
        }
        item(key = "bottom", contentType = "spacer") { Spacer(Modifier.height(8.dp)) }
    }

    editingGoal?.let { metric ->
        GoalDialog(
            metric = metric,
            goals = state.goals,
            onDismiss = { editingGoal = null },
            onSave = { m, v ->
                viewModel.updateGoal(m, v)
                editingGoal = null
            },
        )
    }
}

/** Snackbar olayını gösterir; yeni bildirim eskisinin yerini alır (kuyruk birikmez). */
suspend fun showEvent(event: UiEvent, snackbar: SnackbarHostState, s: AppStrings, onUndo: (String) -> Unit) {
    snackbar.currentSnackbarData?.dismiss()
    when (event) {
        is UiEvent.Deleted -> {
            val r = snackbar.showSnackbar(s.deleted(event.label), actionLabel = s.undo, duration = SnackbarDuration.Long)
            if (r == SnackbarResult.ActionPerformed) onUndo(event.id)
        }
        UiEvent.SaveFailed -> snackbar.showSnackbar(s.saveFailed)
        UiEvent.CustomFoodSaved -> Unit
        UiEvent.CustomFoodLimit -> snackbar.showSnackbar(s.limitReached)
    }
}
