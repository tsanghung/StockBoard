package com.stockboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.*
import com.stockboard.ui.home.HomeScreen
import com.stockboard.ui.theme.StockBoardTheme
import com.stockboard.worker.StockUpdateWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Task 4-2：排程 WorkManager 背景股價刷新（每 15 分鐘）
        scheduleStockUpdateWorker()

        setContent {
            StockBoardTheme {
                HomeScreen()
            }
        }
    }

    /**
     * 使用 ExistingPeriodicWorkPolicy.KEEP，確保：
     * - 若 Worker 尚未排程 → 新增
     * - 若 Worker 已排程 → 保留現有計時器，不重置（避免重啟 APP 導致計時器一直重設）
     *
     * 限制條件：僅在有網路連線時執行（NetworkType.CONNECTED）
     */
    private fun scheduleStockUpdateWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<StockUpdateWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            StockUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
}
