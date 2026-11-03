package com.proteinlifsu.data

import com.proteinlifsu.domain.AppLanguage
import com.proteinlifsu.domain.AppSettings
import com.proteinlifsu.domain.DaySummary
import com.proteinlifsu.domain.Food
import com.proteinlifsu.domain.Goals
import com.proteinlifsu.domain.LogEntry
import com.proteinlifsu.domain.PortionType
import kotlinx.coroutines.flow.Flow

enum class SaveResult { OK, INVALID, LIMIT_REACHED }

/**
 * Veri katmanının tek giriş noktası. Bugün yalnızca yerel Room uygulaması var; ileride bir
 * senkronizasyon uygulaması aynı arayüzü sağlayarak UI'ya dokunmadan takılabilir.
 *
 * Yazma fonksiyonları girdiyi yeniden doğrular (savunma derinliği).
 */
interface TrackerRepository {
    fun observeDay(epochDay: Long): Flow<List<LogEntry>>
    fun observeGoals(): Flow<Goals>
    fun observeDaySummaries(fromEpochDay: Long, toEpochDay: Long): Flow<List<DaySummary>>

    suspend fun addFood(epochDay: Long, food: Food, portion: PortionType, grams: Float, portionLabel: String? = null): Boolean

    /** Kimliğe göre güvenilir besin: sözlük, USDA dizini ya da kendi besinlerin. */
    suspend fun resolveFood(id: String): Food?
    suspend fun addWater(epochDay: Long, label: String, ml: Int): Boolean

    suspend fun delete(id: String): Boolean
    suspend fun restore(id: String): Boolean

    suspend fun updateGoals(goals: Goals): Boolean

    fun observeCustomFoods(): Flow<List<Food>>
    suspend fun addCustomFood(
        name: String,
        proteinPer100g: Float,
        fiberPer100g: Float,
        servingG: Float?,
        recipeSummary: String?,
    ): SaveResult
    suspend fun deleteCustomFood(id: String): Boolean

    fun observeSettings(defaultLanguage: AppLanguage): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)

    /** Eski silme kayıtlarını temizler. */
    suspend fun purgeOldTombstones()
}
