package com.proteinlifsu.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EntryEntity::class, GoalsEntity::class, CustomFoodEntity::class, SettingsEntity::class],
    version = 3,
    exportSchema = true,
    // v2: custom_foods ve settings tabloları eklendi; mevcut kayıtlar korunur.
    // v3: entries.portion_label (USDA ev ölçüsü adı).
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun dao(): TrackerDao

    companion object {
        private const val NAME = "tracker.db"

        /** Veritabanı uygulamanın özel sandbox'ında; ilk sorguda Room'un arka plan iş parçacığında açılır. */
        fun create(context: Context): TrackerDatabase =
            Room.databaseBuilder(context.applicationContext, TrackerDatabase::class.java, NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
