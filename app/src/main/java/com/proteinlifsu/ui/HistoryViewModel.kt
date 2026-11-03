package com.proteinlifsu.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proteinlifsu.ProteinLifSuApp
import com.proteinlifsu.data.TrackerRepository
import com.proteinlifsu.domain.DayTotals
import com.proteinlifsu.domain.Goals
import com.proteinlifsu.domain.PeriodStats
import com.proteinlifsu.domain.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.proteinlifsu.domain.AppLanguage
import com.proteinlifsu.ui.i18n.stringsFor
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId

enum class HistoryMode { DAY, WEEK, MONTH }

@Immutable
data class HistoryUiState(
    val mode: HistoryMode,
    val anchor: Long,
    val from: Long,
    val to: Long,
    val today: Long,
    val byDay: Map<Long, DayTotals> = emptyMap(),
    val goals: Goals = Goals(),
    val stats: PeriodStats? = null,
    /** Dönem başlığı ("Kasım 2026"): arka planda biçimlenir (ana iş parçacığında ICU yüklemesi yok). */
    val title: String = "",
) {
    val canGoNext: Boolean get() = to < today
}

/** Takvim: seçilen dönemin (gün/hafta/ay) günlük toplamları ve hedef tutturma özeti. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: TrackerRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    private fun today(): Long = LocalDate.now(zone()).toEpochDay()

    private val mode = MutableStateFlow(HistoryMode.MONTH)
    private val anchor = MutableStateFlow(today())

    private val language = repository
        .observeSettings(if (java.util.Locale.getDefault().language == "tr") AppLanguage.TR else AppLanguage.EN)
        .map { it.language }
        .distinctUntilChanged()

    val state: StateFlow<HistoryUiState> = combine(mode, anchor) { m, a -> Triple(m, a, range(m, a)) }
        .flatMapLatest { (m, a, r) ->
            combine(repository.observeDaySummaries(r.first, r.second), repository.observeGoals(), language) { sums, goals, lang ->
                val t = today()
                HistoryUiState(
                    mode = m, anchor = a, from = r.first, to = r.second, today = t,
                    byDay = sums.associate { it.epochDay to it.totals },
                    goals = goals,
                    stats = Stats.period(sums, goals, r.first, r.second, t),
                    title = periodTitle(m, a, r.first, r.second, t, stringsFor(lang)),
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            today().let { t -> val r = range(HistoryMode.MONTH, t); HistoryUiState(HistoryMode.MONTH, t, r.first, r.second, t) },
        )

    fun setMode(m: HistoryMode) {
        mode.value = m
    }

    fun select(day: Long) {
        anchor.value = day.coerceAtMost(today())
    }

    fun previous() = anchor.update { shift(mode.value, it, -1) }

    fun next() = anchor.update { minOf(shift(mode.value, it, +1), today()) }

    companion object {
        /** Hafta pazartesi başlar. */
        fun range(m: HistoryMode, anchor: Long): Pair<Long, Long> {
            val d = LocalDate.ofEpochDay(anchor)
            return when (m) {
                HistoryMode.DAY -> anchor to anchor
                HistoryMode.WEEK -> {
                    val start = d.minusDays((d.dayOfWeek.value - 1).toLong())
                    start.toEpochDay() to start.plusDays(6).toEpochDay()
                }
                HistoryMode.MONTH -> {
                    val start = d.withDayOfMonth(1)
                    start.toEpochDay() to start.plusMonths(1).minusDays(1).toEpochDay()
                }
            }
        }

        fun shift(m: HistoryMode, anchor: Long, dir: Int): Long {
            val d = LocalDate.ofEpochDay(anchor)
            return when (m) {
                HistoryMode.DAY -> d.plusDays(dir.toLong())
                HistoryMode.WEEK -> d.plusWeeks(dir.toLong())
                HistoryMode.MONTH -> d.plusMonths(dir.toLong())
            }.toEpochDay()
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ProteinLifSuApp
                HistoryViewModel(app.repository)
            }
        }
    }
}
