package com.stockboard.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stockboard.data.db.AppDatabase
import com.stockboard.data.db.QuoteCache
import com.stockboard.data.network.ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Task 4-2：背景排程 Worker
 * 由 WorkManager 以 PeriodicWorkRequest（15 分鐘）觸發
 *
 * doWork() 執行順序：
 * 1. 呼叫 Yahoo Finance v7 取得台灣 + 日本指數報價
 * 2. 呼叫 Yahoo Chart API (v8) 取得美股指數報價 (與 HomeViewModel 來源對齊)
 * 3. 將所有結果批次寫入 Room quote_cache
 */
class StockUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "StockUpdateWorker"

        // 統一對齊 HomeViewModel 的 Yahoo Finance 美股指數代號
        private val US_SYMBOLS = listOf("^DJI", "^IXIC", "^GSPC", "^SOX", "TSM")

        // 台灣 + 日本指數
        private const val YAHOO_SYMBOLS = "^TWII,^TPEXCD,TXF1=F,^N225"
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val cacheDao = db.quoteCacheDao()
            val now = System.currentTimeMillis()
            val newCaches = mutableListOf<QuoteCache>()

            // Step 1：Yahoo Finance v7 — 台灣 + 日本大盤
            try {
                val response = ApiClient.yahooFinanceService.getQuotes(YAHOO_SYMBOLS)
                response.quoteResponse?.result?.forEach { quote ->
                    newCaches.add(
                        QuoteCache(
                            symbol = quote.symbol,
                            price = quote.regularMarketPrice ?: 0.0,
                            change = quote.regularMarketChange ?: 0.0,
                            changePct = quote.regularMarketChangePercent ?: 0.0,
                            marketTime = quote.regularMarketTime ?: now,
                            updatedAt = now
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(WORK_NAME, "Fetch Yahoo v7 failed: ${e.message}")
            }

            // Step 2：Yahoo Chart API (v8) — 美股大盤 (同步 HomeViewModel 邏輯)
            try {
                coroutineScope {
                    val usDeferred = US_SYMBOLS.map { symbol ->
                        async {
                            try {
                                val encoded = symbol.replace("^", "%5E")
                                val response = ApiClient.yahooChartService.getChart(encoded)
                                val meta = response.chart?.result?.firstOrNull()?.meta

                                val price = meta?.regularMarketPrice ?: 0.0
                                val prev = meta?.previousClose ?: meta?.chartPreviousClose ?: 0.0
                                val change = if (price != 0.0 && prev != 0.0) price - prev else 0.0
                                val changePct = if (change != 0.0 && prev != 0.0) (change / prev) * 100 else 0.0
                                val marketTime = meta?.regularMarketTime?.toLong() ?: now

                                if (price != 0.0) {
                                    QuoteCache(
                                        symbol = symbol,
                                        price = price,
                                        change = change,
                                        changePct = changePct,
                                        marketTime = marketTime,
                                        updatedAt = now
                                    )
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }

                    // 等待所有美股 API 請求完成並過濾失敗項目
                    usDeferred.awaitAll().filterNotNull().forEach { cache ->
                        newCaches.add(cache)
                    }
                }
            } catch (e: Exception) {
                Log.e(WORK_NAME, "Fetch Yahoo Chart failed: ${e.message}")
            }

            // Step 3：批次寫入 Room quote_cache
            if (newCaches.isNotEmpty()) {
                cacheDao.insertAll(newCaches)
            }

            Result.success()
        } catch (e: Exception) {
            // 發生不可預期錯誤時回傳 retry，WorkManager 自動排程重試
            Result.retry()
        }
    }
}
