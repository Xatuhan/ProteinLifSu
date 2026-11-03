package com.proteinlifsu.data

import com.proteinlifsu.data.db.CustomFoodEntity
import com.proteinlifsu.data.db.EntryEntity
import com.proteinlifsu.data.db.GoalsEntity
import com.proteinlifsu.data.db.SettingsEntity
import com.proteinlifsu.data.db.TrackerDao
import com.proteinlifsu.domain.AppLanguage
import com.proteinlifsu.domain.AppSettings
import com.proteinlifsu.domain.DaySummary
import com.proteinlifsu.domain.DayTotals
import com.proteinlifsu.domain.EntryKind
import com.proteinlifsu.domain.Food
import com.proteinlifsu.domain.FoodCatalog
import com.proteinlifsu.domain.Goals
import com.proteinlifsu.domain.InputLimits
import com.proteinlifsu.domain.InputValidator
import com.proteinlifsu.domain.LogEntry
import com.proteinlifsu.domain.Metric
import com.proteinlifsu.domain.PieceUnit
import com.proteinlifsu.domain.PortionType
import com.proteinlifsu.domain.ThemeMode
import com.proteinlifsu.domain.Validated
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class RoomTrackerRepository(
    private val dao: TrackerDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val compute: CoroutineDispatcher = Dispatchers.Default,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val clock: () -> Long = System::currentTimeMillis,
    private val usda: suspend () -> com.proteinlifsu.domain.FoodIndex = { com.proteinlifsu.domain.FoodIndex.EMPTY },
) : TrackerRepository {

    override fun observeDay(epochDay: Long): Flow<List<LogEntry>> =
        dao.observeDay(epochDay)
            .distinctUntilChanged()
            .map { rows -> rows.map(::toLogEntry) }
            .flowOn(compute)

    override fun observeGoals(): Flow<Goals> =
        dao.observeGoals()
            .map { g -> g?.let { Goals(it.proteinG, it.fiberG, it.waterMl) } ?: Goals() }
            .distinctUntilChanged()
            .flowOn(compute)

    override fun observeDaySummaries(fromEpochDay: Long, toEpochDay: Long): Flow<List<DaySummary>> =
        dao.observeDaySums(fromEpochDay, toEpochDay)
            .distinctUntilChanged()
            .map { rows ->
                rows.map {
                    DaySummary(
                        it.epochDay,
                        DayTotals(
                            it.protein.toFloat(),
                            it.fiber.toFloat(),
                            it.water.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                        ),
                    )
                }
            }
            .flowOn(compute)

    override suspend fun addFood(epochDay: Long, food: Food, portion: PortionType, grams: Float, portionLabel: String?): Boolean {
        // Değerler çağırandan değil, güvenilir kaynaktan (sözlük, USDA dizini ya da veritabanı) okunur.
        val trusted = resolveFood(food.id) ?: return false
        if (InputValidator.checkFoodGrams(grams, trusted.maxGramsPerEntry) !is Validated.Ok) return false
        val n = trusted.nutrientsFor(grams)
        if (!n.proteinG.isFinite() || !n.fiberG.isFinite()) return false
        val now = clock()
        val entry = EntryEntity(
            id = UUID.randomUUID().toString(),
            epochDay = epochDay,
            createdAt = timestampFor(epochDay, now),
            kind = EntryKind.FOOD.name,
            label = trusted.nameTr.take(InputLimits.MAX_LABEL_CHARS),
            foodId = trusted.id,
            portion = portion.name,
            portionLabel = portionLabel?.let { InputValidator.sanitizeName(it) }?.takeIf { portion == PortionType.HOUSEHOLD },
            grams = grams,
            proteinG = n.proteinG,
            fiberG = n.fiberG,
            // Çorba/içecek içindeki su, su göstergesine de sayılır.
            waterMl = trusted.waterMlFor(grams),
            updatedAt = now,
        )
        withContext(io) { dao.insert(entry) }
        return true
    }

    override suspend fun resolveFood(id: String): Food? = when {
        id.startsWith(Food.USDA_PREFIX) -> withContext(compute) { usda().find(id) }
        id.startsWith(Food.CUSTOM_PREFIX) -> withContext(io) { dao.getCustomFood(id) }?.let(::toFood)
        else -> FoodCatalog.find(id)
    }

    override suspend fun addWater(epochDay: Long, label: String, ml: Int): Boolean {
        if (InputValidator.checkWaterMl(ml) !is Validated.Ok) return false
        val now = clock()
        val entry = EntryEntity(
            id = UUID.randomUUID().toString(),
            epochDay = epochDay,
            createdAt = timestampFor(epochDay, now),
            kind = EntryKind.WATER.name,
            label = InputValidator.sanitizeName(label),
            foodId = null,
            portion = null,
            grams = 0f,
            proteinG = 0f,
            fiberG = 0f,
            waterMl = ml,
            updatedAt = now,
        )
        withContext(io) { dao.insert(entry) }
        return true
    }

    override suspend fun delete(id: String): Boolean =
        withContext(io) { dao.softDelete(id, clock()) > 0 }

    override suspend fun restore(id: String): Boolean =
        withContext(io) { dao.restore(id, clock()) > 0 }

    override suspend fun updateGoals(goals: Goals): Boolean {
        val ok = InputValidator.checkGoal(Metric.PROTEIN, goals.proteinG) is Validated.Ok &&
            InputValidator.checkGoal(Metric.FIBER, goals.fiberG) is Validated.Ok &&
            InputValidator.checkGoal(Metric.WATER, goals.waterMl.toFloat()) is Validated.Ok
        if (!ok) return false
        withContext(io) {
            dao.upsertGoals(
                GoalsEntity(
                    proteinG = goals.proteinG,
                    fiberG = goals.fiberG,
                    waterMl = goals.waterMl,
                    updatedAt = clock(),
                ),
            )
        }
        return true
    }

    override fun observeCustomFoods(): Flow<List<Food>> =
        dao.observeCustomFoods()
            .distinctUntilChanged()
            .map { rows -> rows.map(::toFood) }
            .flowOn(compute)

    override suspend fun addCustomFood(
        name: String,
        proteinPer100g: Float,
        fiberPer100g: Float,
        servingG: Float?,
        recipeSummary: String?,
    ): SaveResult {
        val cleanName = (InputValidator.validateName(name) as? Validated.Ok)?.value ?: return SaveResult.INVALID
        if (InputValidator.checkPer100(proteinPer100g) !is Validated.Ok) return SaveResult.INVALID
        if (InputValidator.checkPer100(fiberPer100g) !is Validated.Ok) return SaveResult.INVALID
        if (proteinPer100g + fiberPer100g > 100f) return SaveResult.INVALID
        if (servingG != null && InputValidator.checkFoodGrams(servingG) !is Validated.Ok) return SaveResult.INVALID
        val summary = recipeSummary?.let { InputValidator.sanitizeName(it.take(RECIPE_SUMMARY_MAX)) }
        return withContext(io) {
            if (dao.customFoodCount() >= InputLimits.MAX_CUSTOM_FOODS) return@withContext SaveResult.LIMIT_REACHED
            dao.insertCustomFood(
                CustomFoodEntity(
                    id = Food.CUSTOM_PREFIX + UUID.randomUUID().toString(),
                    name = cleanName,
                    proteinPer100g = proteinPer100g,
                    fiberPer100g = fiberPer100g,
                    servingG = servingG,
                    recipeSummary = summary,
                    updatedAt = clock(),
                ),
            )
            SaveResult.OK
        }
    }

    override suspend fun deleteCustomFood(id: String): Boolean =
        withContext(io) { dao.softDeleteCustomFood(id, clock()) > 0 }

    override fun observeSettings(defaultLanguage: AppLanguage): Flow<AppSettings> =
        dao.observeSettings()
            .map { s ->
                AppSettings(
                    language = s?.language?.let { v -> AppLanguage.entries.firstOrNull { it.name == v } } ?: defaultLanguage,
                    theme = s?.theme?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.SYSTEM,
                )
            }
            .distinctUntilChanged()
            .flowOn(compute)

    override suspend fun updateSettings(settings: AppSettings) {
        withContext(io) { dao.upsertSettings(SettingsEntity(language = settings.language.name, theme = settings.theme.name)) }
    }

    override suspend fun purgeOldTombstones() {
        withContext(io) {
            val before = clock() - TOMBSTONE_TTL_MS
            dao.purgeTombstones(before)
            dao.purgeCustomFoodTombstones(before)
        }
    }

    /** Geçmiş bir güne eklenen kayıt o günün "şu anki saatine" yazılır. */
    private fun timestampFor(epochDay: Long, now: Long): Long {
        val z = zone()
        // LocalDate.ofInstant API 34 ister; minSdk 26 için ZonedDateTime üzerinden gidilir.
        val nowZoned = Instant.ofEpochMilli(now).atZone(z)
        if (epochDay == nowZoned.toLocalDate().toEpochDay()) return now
        val time: LocalTime = nowZoned.toLocalTime()
        return LocalDate.ofEpochDay(epochDay).atTime(time).atZone(z).toInstant().toEpochMilli()
    }

    private fun toLogEntry(e: EntryEntity): LogEntry = LogEntry(
        id = e.id,
        kind = if (e.kind == EntryKind.WATER.name) EntryKind.WATER else EntryKind.FOOD,
        label = e.label,
        foodId = e.foodId,
        portion = e.portion?.let { name -> PortionType.entries.firstOrNull { it.name == name } },
        portionLabel = e.portionLabel,
        createdAt = e.createdAt,
        grams = e.grams,
        proteinG = e.proteinG,
        fiberG = e.fiberG,
        waterMl = e.waterMl,
    )

    private fun toFood(e: CustomFoodEntity): Food = Food(
        id = e.id,
        nameTr = e.name,
        nameEn = e.name,
        proteinPer100g = e.proteinPer100g,
        fiberPer100g = e.fiberPer100g,
        portions = e.servingG?.let { mapOf(PortionType.PIECE to it.roundToInt().coerceAtLeast(1)) } ?: emptyMap(),
        piece = PieceUnit.SERVING,
        noteTr = e.recipeSummary,
        noteEn = e.recipeSummary,
        isCustom = true,
    )

    companion object {
        private val TOMBSTONE_TTL_MS = TimeUnit.DAYS.toMillis(30)
        private const val RECIPE_SUMMARY_MAX = 64
    }
}
