package com.stockboard

import android.app.Application
import com.google.android.gms.ads.MobileAds

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
            // 可在此記錄各廣告網路的初始化結果（選用）
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapter, status) in statusMap) {
                android.util.Log.d("AdMob", "Adapter: $adapter, Status: ${status.initializationState}")
            }
        }
    }
}
