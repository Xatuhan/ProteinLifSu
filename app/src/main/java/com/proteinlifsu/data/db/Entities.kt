package com.proteinlifsu.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * İleride bulut senkronizasyonu için ortak meta alanlar.
 * - [updatedAt]: son yerel değişiklik (epoch ms) — son-yazan-kazanır çakışma çözümü için.
 * - [isSynced]: sunucuya gönderildi mi; her yerel değişiklikte false olur.
 */
interface Syncable {
    val updatedAt: Long
    val isSynced: Boolean
}

@Entity(
    tableName = "entries",
    indices = [Index(value = ["epoch_day", "is_deleted"])],
)
data class EntryEntity(
    /** UUID: cihazlar arası birleştirmede çakışmayan kimlik. */
    @PrimaryKey val id: String,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** "FOOD" ya da "WATER" (EntryKind.name). */
    val kind: String,
    val label: String,
    @ColumnInfo(name = "food_id") val foodId: String?,
    /** PortionType.name; su için null. */
    val portion: String?,
    /** HOUSEHOLD ölçüsünün adı (v3). */
    @ColumnInfo(name = "portion_label") val portionLabel: String? = null,
    val grams: Float,
    @ColumnInfo(name = "protein_g") val proteinG: Float,
    @ColumnInfo(name = "fiber_g") val fiberG: Float,
    @ColumnInfo(name = "water_ml") val waterMl: Int,
    @ColumnInfo(name = "updated_at") override val updatedAt: Long,
    @ColumnInfo(name = "is_synced") override val isSynced: Boolean = false,
    /** Silinen kayıt hemen yok edilmez (geri alma + ileride sunucuya silme bilgisi). */
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
) : Syncable

@Entity(tableName = "goals")
data class GoalsEntity(
    /** Tek satır. */
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "protein_g") val proteinG: Float,
    @ColumnInfo(name = "fiber_g") val fiberG: Float,
    @ColumnInfo(name = "water_ml") val waterMl: Int,
    @ColumnInfo(name = "updated_at") override val updatedAt: Long,
    @ColumnInfo(name = "is_synced") override val isSynced: Boolean = false,
) : Syncable {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/** Kullanıcının eklediği besin (etiketten ya da tariften hesaplanmış, 100 g başına). */
@Entity(tableName = "custom_foods")
data class CustomFoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "protein_per_100g") val proteinPer100g: Float,
    @ColumnInfo(name = "fiber_per_100g") val fiberPer100g: Float,
    /** İsteğe bağlı "1 porsiyon" gramı. */
    @ColumnInfo(name = "serving_g") val servingG: Float?,
    /** Tariften hesaplandıysa malzeme özeti (yalnız gösterim). */
    @ColumnInfo(name = "recipe_summary") val recipeSummary: String?,
    @ColumnInfo(name = "updated_at") override val updatedAt: Long,
    @ColumnInfo(name = "is_synced") override val isSynced: Boolean = false,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
) : Syncable

/** Uygulama ayarları (tek satır). Cihaza özeldir; senkronize edilmez. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    /** AppLanguage.name */
    val language: String,
    /** ThemeMode.name */
    val theme: String,
)

/** Takvim sorgusunun satırı. */
data class DaySumRow(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "protein") val protein: Double,
    @ColumnInfo(name = "fiber") val fiber: Double,
    @ColumnInfo(name = "water") val water: Long,
)
