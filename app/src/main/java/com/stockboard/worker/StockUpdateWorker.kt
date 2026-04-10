package com.stockboard.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stockboard.data.db.AppDatabase
import com.stockboard.data.db.QuoteCache
import com.stockboard.data.network.ApiClient
import com.stockboard.network.FinnhubFetcher

/**
 * Task 4-2：背景排程 Worker
 * 由 WorkManager 以 PeriodicWorkRequest（15 分鐘）觸發
 *
 * doWork() 執行順序：
 * 1. 呼叫 Yahoo Finance v7 取得台灣 + 日本指數報價
 * 2. 透過 FinnhubFetcher（含節流）取得美股指數報價
 * 3. 將所有結果批次寫入 Room quote_cache
 *
 * APP 重新開啟時，HomeViewModel 會先從 quote_cache 讀取快取顯示，
 * 再進行背景刷新以取得最新數據。
 */
class StockUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "StockUpdateWorker"

        // 美股大盤追蹤清單（與 HomeViewModel 保持一致）
        private val US_SYMBOLS = listOf("DIA", "QQQ", "SPY", "SOXX", "TSM")

        // 台灣 + 日本指數
        private const val YAHOO_SYMBOLS = "^TWII,^TPEXCD,TXF1=F,^N225"
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val cacheDao = db.quoteCacheDao()
            val now = System.currentTimeMillis()
            val newCaches = mutableListOf<QuoteCache>()

            // Step 1：Yahoo Finance — 台灣 + 日本
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
                // Yahoo 失敗不影響 Finnhub 部分，繼續執行
            }

            // Step 2：Finnhub — 美股大盤（含節流防護）
            val fetcher = FinnhubFetcher()
            fetcher.fetchWithThrottling(
                marketIndices = US_SYMBOLS,
                watchlistedSymbols = emptyList()
            ) { symbol ->
                try {
                    // TODO: 正式版從 local.properties 讀取 API Key
                    val quote = ApiClient.finnhubService.getQuote(symbol, "demo_token")
                    newCaches.add(
                        QuoteCache(
                            symbol = symbol,
                            price = quote.currentPrice ?: 0.0,
                            change = quote.change ?: 0.0,
                            changePct = quote.changePercent ?: 0.0,
                            marketTime = quote.timestamp ?: now,
                            updatedAt = now
                        )
                    )
                } catch (e: Exception) {
                    // 單一 symbol 失敗忽略
                }
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
