package com.stockboard

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.stockboard.data.db.AppDatabase
import com.stockboard.data.db.WatchlistItem
import com.stockboard.util.MarketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Task 6-1：AdMob Application 類別
 * 在 APP 啟動最早期（Application.onCreate）初始化 AdMob SDK
 *
 * 必須在 AndroidManifest.xml 中以 android:name=".StockBoardApplication" 宣告
 */
class StockBoardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化 AdMob SDK（非同步，不阻塞主執行緒）
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapter, status) in statusMap) {
                android.util.Log.d("AdMob", "Adapter: $adapter, Status: ${status.initializationState}")
            }
        }
        seedInitialWatchlist()
    }

    /**
     * 分版本植入預設自選股，每個版本旗標只執行一次
     * v1：台積電(2330)、鈊象(3293)
     * v2：NVDA、GOOG
     */
    private fun seedInitialWatchlist() {
        val prefs = getSharedPreferences("stockboard_prefs", MODE_PRIVATE)

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(applicationContext).watchlistDao()

            if (!prefs.getBoolean("watchlist_seeded_v1", false)) {
                listOf(
                    WatchlistItem(symbol = "2330", name = "台積電", market = MarketType.TAIWAN, sortOrder = 1, isFixed = false),
                    WatchlistItem(symbol = "3293", name = "鈊象",   market = MarketType.TAIWAN, sortOrder = 2, isFixed = false)
                ).forEach { if (dao.countBySymbol(it.symbol, it.market) == 0) dao.insertItem(it) }
                prefs.edit().putBoolean("watchlist_seeded_v1", true).apply()
            }

            if (!prefs.getBoolean("watchlist_seeded_v2", false)) {
                listOf(
                    WatchlistItem(symbol = "NVDA", name = "NVIDIA",  market = MarketType.US, sortOrder = 3, isFixed = false),
                    WatchlistItem(symbol = "GOOG", name = "Alphabet", market = MarketType.US, sortOrder = 4, isFixed = false)
                ).forEach { if (dao.countBySymbol(it.symbol, it.market) == 0) dao.insertItem(it) }
                prefs.edit().putBoolean("watchlist_seeded_v2", true).apply()
            }
        }
    }
}
