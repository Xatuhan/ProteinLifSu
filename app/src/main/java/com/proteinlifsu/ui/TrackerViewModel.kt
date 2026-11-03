package com.proteinlifsu.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proteinlifsu.ProteinLifSuApp
import com.proteinlifsu.data.SaveResult
import com.proteinlifsu.data.TrackerRepository
import com.proteinlifsu.domain.AppLanguage
import com.proteinlifsu.domain.AppSettings
import com.proteinlifsu.domain.DayTotals
import com.proteinlifsu.domain.Food
import com.proteinlifsu.domain.Goals
import com.proteinlifsu.domain.InputValidator
import com.proteinlifsu.domain.LogEntry
import com.proteinlifsu.domain.Metric
import com.proteinlifsu.domain.PortionType
import com.proteinlifsu.domain.ThemeMode
import com.proteinlifsu.domain.Validated
import com.proteinlifsu.domain.grouped
import com.proteinlifsu.domain.totals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.proteinlifsu.ui.i18n.stringsFor
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.proteinlifsu.data.FoodIndexProvider
import com.proteinlifsu.domain.FoodCatalog
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

@Immutable
data class TrackerUiState(
    val epochDay: Long,
    val today: Long,
    val goals: Goals = Goals(),
    val totals: DayTotals = DayTotals(),
    val entries: List<LogEntry> = emptyList(),
    val groups: List<com.proteinlifsu.domain.LogGroup> = emptyList(),
    val loaded: Boolean = false,
    /**
     * "Bugün" / "2 Kasım Pazartesi": arka planda biçimlenir. java.time'ın ay/gün adlarını ilk
     * kullanımda ICU'dan yüklemesi ~1,6 sn sürebildiği için ana iş parçacığında yapılmaz (ANR kök nedeni).
     */
    val dateTitle: String = "",
    val dateSubtitle: String = "",
)

sealed interface UiEvent {
    data class Deleted(val id: String, val label: String) : UiEvent
    data object SaveFailed : UiEvent
    data object CustomFoodSaved : UiEvent
    data object CustomFoodLimit : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(
    private val repository: TrackerRepository,
    private val foodIndex: FoodIndexProvider? = null,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    fun today(): Long = LocalDate.now(zone()).toEpochDay()

    private val selectedDay = MutableStateFlow(today())

    /** Tek seferlik olaylar (snackbar). Sınırlı kapasite: kuyruk sınırsız büyüyemez. */
    private val _events = Channel<UiEvent>(capacity = 16)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    /** Sistem dili Türkçeyse Türkçe, değilse İngilizce başlar. */
    private val defaultLanguage: AppLanguage =
        if (Locale.getDefault().language == "tr") AppLanguage.TR else AppLanguage.EN

    /** null = henüz okunmadı (ilk karede yanlış dil/tema yanıp sönmesin diye). */
    val settings: StateFlow<AppSettings?> = repository.observeSettings(defaultLanguage)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val language: Flow<AppLanguage> = settings
        .map { it?.language ?: defaultLanguage }
        .distinctUntilChanged()

    val state: StateFlow<TrackerUiState> = selectedDay
        .flatMapLatest { day ->
            combine(repository.observeDay(day), repository.observeGoals(), language) { entries, goals, lang ->
                val today = today()
                // Tarih metinleri burada (Default iş parçacığında) biçimlenir.
                val (title, subtitle) = com.proteinlifsu.ui.components.dateLabels(day, today, stringsFor(lang))
                TrackerUiState(
                    day, today, goals, entries.totals(), entries, entries.grouped(), loaded = true,
                    dateTitle = title, dateSubtitle = subtitle,
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackerUiState(today(), today()))

    val customFoods: StateFlow<List<Food>> = repository.observeCustomFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun previousDay() = selectedDay.update { it - 1 }

    fun nextDay() = selectedDay.update { (it + 1).coerceAtMost(today()) }

    fun goToday() {
        selectedDay.value = today()
    }

    fun goToDay(epochDay: Long) {
        selectedDay.value = epochDay.coerceAtMost(today())
    }

    fun addFood(food: Food, portion: PortionType, grams: Float, portionLabel: String? = null) {
        if (InputValidator.checkFoodGrams(grams, food.maxGramsPerEntry) !is Validated.Ok) return
        val day = selectedDay.value
        viewModelScope.launch {
            if (!repository.addFood(day, food, portion, grams, portionLabel)) _events.trySend(UiEvent.SaveFailed)
        }
    }

    /**
     * Besin arama: önce kendi besinlerin, sonra elle seçilmiş sözlük, sonra USDA tam veri seti.
     * Arka planda (Default) çalışır; boş sorguda USDA listelenmez.
     */
    suspend fun searchFoods(query: String): List<Food> = withContext(Dispatchers.Default) {
        val local = FoodCatalog.search(query, customFoods.value)
        if (query.isBlank()) local else local + (foodIndex?.get()?.search(query, USDA_LIMIT) ?: emptyList())
    }

    suspend fun resolveFood(id: String): Food? =
        customFoods.value.firstOrNull { it.id == id } ?: repository.resolveFood(id)

    /** Yüklenmişse eşzamanlı bulma (liste görünümü için; yüklenmemişse null). */
    fun findLoaded(id: String?): Food? =
        FoodCatalog.find(id) ?: foodIndex?.current?.find(id) ?: customFoods.value.firstOrNull { it.id == id }

    fun addWater(label: String, ml: Int) {
        if (InputValidator.checkWaterMl(ml) !is Validated.Ok) return
        val day = selectedDay.value
        viewModelScope.launch {
            if (!repository.addWater(day, label, ml)) _events.trySend(UiEvent.SaveFailed)
        }
    }

    fun delete(entry: LogEntry, displayLabel: String) {
        viewModelScope.launch {
            if (repository.delete(entry.id)) _events.trySend(UiEvent.Deleted(entry.id, displayLabel))
        }
    }

    fun undoDelete(id: String) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun updateGoal(metric: Metric, value: Float) {
        if (InputValidator.checkGoal(metric, value) !is Validated.Ok) return
        val current = state.value.goals
        val next = when (metric) {
            Metric.PROTEIN -> current.copy(proteinG = value)
            Metric.FIBER -> current.copy(fiberG = value)
            Metric.WATER -> current.copy(waterMl = value.roundToInt())
        }
        viewModelScope.launch {
            if (!repository.updateGoals(next)) _events.trySend(UiEvent.SaveFailed)
        }
    }

    fun setLanguage(lang: AppLanguage) = updateSettings { it.copy(language = lang) }

    fun setTheme(theme: ThemeMode) = updateSettings { it.copy(theme = theme) }

    private fun updateSettings(change: (AppSettings) -> AppSettings) {
        val current = settings.value ?: AppSettings(defaultLanguage)
        viewModelScope.launch { repository.updateSettings(change(current)) }
    }

    /** Kaydedildiyse true döner (ekran kapanabilir). */
    fun saveCustomFood(name: String, proteinPer100g: Float, fiberPer100g: Float, servingG: Float?, recipeSummary: String?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.addCustomFood(name, proteinPer100g, fiberPer100g, servingG, recipeSummary)
            when (result) {
                SaveResult.OK -> _events.trySend(UiEvent.CustomFoodSaved)
                SaveResult.LIMIT_REACHED -> _events.trySend(UiEvent.CustomFoodLimit)
                SaveResult.INVALID -> _events.trySend(UiEvent.SaveFailed)
            }
            onDone(result == SaveResult.OK)
        }
    }

    fun deleteCustomFood(id: String) {
        viewModelScope.launch { repository.deleteCustomFood(id) }
    }

    companion object {
        private const val USDA_LIMIT = 60

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ProteinLifSuApp
                TrackerViewModel(app.repository, app.foodIndex)
            }
        }
    }
}
