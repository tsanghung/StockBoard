package com.stockboard.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stockboard.data.db.AppDatabase
import com.stockboard.data.db.NewsEntity
import com.stockboard.data.db.QuoteCache
import com.stockboard.data.network.ApiClient
import com.stockboard.data.network.NewsRssParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.security.MessageDigest

/**
 * Task 4-2：背景排程 Worker
 * 由 WorkManager 以 PeriodicWorkRequest（15 分鐘）觸發
 *
 * doWork() 執行順序：
 * 1. 呼叫 Yahoo Finance v7 取得台灣 + 日本指數報價
 * 2. 呼叫 Yahoo Chart API (v8) 取得美股指數報價 (與 HomeViewModel 來源對齊)
 * 3. 將所有結果批次寫入 Room quote_cache
 * 4. 批次抓取 4 個新聞 RSS 來源，寫入 Room news_cache（新增）
 */
class StockUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "StockUpdateWorker"

        private val US_SYMBOLS = listOf("^DJI", "^IXIC", "^GSPC", "^SOX", "TSM")
        private const val YAHOO_SYMBOLS = "^TWII,^TPEXCD,TXF1=F,^N225"

        /** 新聞 RSS 來源對應表，key = source 欄位值，value = RSS URL */
        private val NEWS_SOURCES = mapOf(
            "Yahoo"    to "https://tw.news.yahoo.com/rss/finance",
            "CNA"      to "https://www.cna.com.tw/rss/aall.xml",
            "TechNews" to "https://technews.tw/feed/",
            "Inside"   to "https://www.inside.com.tw/feed"
        )
    }

    /**
     * URL 轉 MD5，作為 NewsEntity 主鍵以防重複。
     * 使用 and 0xFF 遮罩，將 Kotlin Signed Byte 轉為正整數後格式化，
     * 避免負值（如 -1）被展開為 ffffffff 而破壞 32 字元的 MD5 格式。
     */
    private fun String.toMd5(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
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

            // Step 2：Yahoo Chart API (v8) — 美股大盤
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

            // Step 4：批次抓取新聞 RSS，寫入 Room news_cache
            // 獨立 try-catch：新聞失敗不影響股價任務的成功結果，也不觸發整批 retry
            // （retry 會重抓股價、浪費 API quota，且若 QuoteCache 主鍵非 symbol 會產生重複列）
            try {
                fetchAndSaveNews(db)
            } catch (e: Exception) {
                Log.e(WORK_NAME, "news task failed: ${e.message}")
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /** 依序抓取 4 個 RSS 來源，解析後批次寫入 DB，並清除 48 小時前舊新聞 */
    private suspend fun fetchAndSaveNews(db: AppDatabase) {
        val newsDao = db.newsDao()
        val cutoff48h = System.currentTimeMillis() - 48 * 3600 * 1000L

        // 清除過舊的新聞
        newsDao.deleteOldNews(cutoff48h)

        NEWS_SOURCES.forEach { (sourceName, rssUrl) ->
            try {
                // .use {} 包裹 ResponseBody：ResponseBody.string() 是破壞性單次讀取，
                // .use {} 確保無論成功或例外都呼叫 close()，防止底層 BufferedSource 洩漏
                val xml = ApiClient.rssNewsService.fetchRss(rssUrl).use { it.string() }
                val parsedItems = NewsRssParser.parse(xml)

                val entities = parsedItems.map { item ->
                    NewsEntity(
                        id = item.link.toMd5(),
                        title = item.title,
                        source = sourceName,
                        publishTime = item.publishTimeMs,
                        url = item.link,
                        imageUrl = item.imageUrl
                    )
                }

                if (entities.isNotEmpty()) {
                    newsDao.insertAll(entities)
                    Log.d(WORK_NAME, "新聞已寫入 $sourceName：${entities.size} 則")
                }
            } catch (e: Exception) {
                Log.e(WORK_NAME, "抓取新聞失敗 $sourceName：${e.message}")
            }
        }
    }
}
