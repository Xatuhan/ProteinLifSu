package com.proteinlifsu.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Tüm sorgular parametreli (bağlı değişken); dize birleştirme ile SQL kurulmaz. */
@Dao
interface TrackerDao {

    @Query(
        "SELECT * FROM entries WHERE epoch_day = :epochDay AND is_deleted = 0 " +
            "ORDER BY created_at DESC",
    )
    fun observeDay(epochDay: Long): Flow<List<EntryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: EntryEntity)

    @Query("UPDATE entries SET is_deleted = 1, is_synced = 0, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long): Int

    @Query("UPDATE entries SET is_deleted = 0, is_synced = 0, updated_at = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long): Int

    /** Silme kayıtları sınırsız büyümesin: eşik öncesindekiler kalıcı olarak silinir. */
    @Query("DELETE FROM entries WHERE is_deleted = 1 AND updated_at < :before")
    suspend fun purgeTombstones(before: Long): Int

    @Query("SELECT * FROM goals WHERE id = 1")
    fun observeGoals(): Flow<GoalsEntity?>

    @Upsert
    suspend fun upsertGoals(goals: GoalsEntity)

    // ---- Takvim ----

    @Query(
        "SELECT epoch_day, SUM(protein_g) AS protein, SUM(fiber_g) AS fiber, SUM(water_ml) AS water " +
            "FROM entries WHERE is_deleted = 0 AND epoch_day BETWEEN :from AND :to " +
            "GROUP BY epoch_day ORDER BY epoch_day",
    )
    fun observeDaySums(from: Long, to: Long): Flow<List<DaySumRow>>

    // ---- Kendi besinlerim ----

    @Query("SELECT * FROM custom_foods WHERE is_deleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeCustomFoods(): Flow<List<CustomFoodEntity>>

    @Query("SELECT * FROM custom_foods WHERE id = :id AND is_deleted = 0")
    suspend fun getCustomFood(id: String): CustomFoodEntity?

    @Query("SELECT COUNT(*) FROM custom_foods WHERE is_deleted = 0")
    suspend fun customFoodCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomFood(food: CustomFoodEntity)

    @Query("UPDATE custom_foods SET is_deleted = 1, is_synced = 0, updated_at = :now WHERE id = :id")
    suspend fun softDeleteCustomFood(id: String, now: Long): Int

    @Query("DELETE FROM custom_foods WHERE is_deleted = 1 AND updated_at < :before")
    suspend fun purgeCustomFoodTombstones(before: Long): Int

    // ---- Ayarlar ----

    @Query("SELECT * FROM settings WHERE id = 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Upsert
    suspend fun upsertSettings(settings: SettingsEntity)
}
