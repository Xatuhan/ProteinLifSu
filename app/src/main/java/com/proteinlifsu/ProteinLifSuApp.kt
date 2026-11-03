package com.proteinlifsu

import android.app.Application
import android.os.StrictMode
import com.proteinlifsu.data.FoodIndexProvider
import com.proteinlifsu.data.RoomTrackerRepository
import com.proteinlifsu.data.TrackerRepository
import com.proteinlifsu.data.db.TrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Elle bağımlılık enjeksiyonu: DI kütüphanesi yok, tek veritabanı + tek repository. */
class ProteinLifSuApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** USDA tam veri seti; açılışta arka planda yüklenir. */
    val foodIndex: FoodIndexProvider by lazy { FoodIndexProvider(this, appScope) }

    val repository: TrackerRepository by lazy {
        RoomTrackerRepository(TrackerDatabase.create(this).dao(), usda = { foodIndex.get() })
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Yalnız debug: ana iş parçacığında disk/ağ erişimi olursa hemen görünür olsun.
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build(),
            )
        }
        // USDA ön yüklemesi burada başlamaz: MainActivity ilk kareden sonra, ana iş parçacığı
        // boşaldığında başlatır (açılışta işlemci paylaşımı azalsın). Arama gerekirse zaten bekler.
        appScope.launch { repository.purgeOldTombstones() }
    }
}
