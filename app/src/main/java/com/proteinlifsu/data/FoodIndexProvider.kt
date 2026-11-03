package com.proteinlifsu.data

import android.content.Context
import com.proteinlifsu.domain.FoodIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * USDA besin dosyasını (assets/usda_foods.tsv, ≈2,6 MB) arka planda bir kez okur.
 * Okuma ana iş parçacığında yapılmaz; hazır olana kadar [current] null'dır ve arama yalnız
 * yerleşik/kendi besinlerinde çalışır.
 */
class FoodIndexProvider(context: Context, scope: CoroutineScope) {

    private val appContext = context.applicationContext

    @Volatile
    var current: FoodIndex? = null
        private set

    private val loading: Deferred<FoodIndex> = scope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
        // Ayrıştırma (≈13 400 satır) ilk karelerin çizildiği ana iş parçacığıyla işlemci için yarışmasın:
        // bu iş parçacığı geçici olarak arka plan önceliğine alınır, iş bitince eski önceliğe döner.
        val tid = android.os.Process.myTid()
        val oldPriority = android.os.Process.getThreadPriority(tid)
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        try {
            val index = try {
                appContext.assets.open(ASSET).bufferedReader(Charsets.UTF_8).useLines { FoodIndex.parse(it) }
            } catch (e: java.io.IOException) {
                FoodIndex.EMPTY
            }
            current = index
            index
        } finally {
            android.os.Process.setThreadPriority(oldPriority)
        }
    }

    fun preload() {
        loading.start()
    }

    suspend fun get(): FoodIndex = loading.await()

    companion object {
        const val ASSET = "usda_foods.tsv"
    }
}
