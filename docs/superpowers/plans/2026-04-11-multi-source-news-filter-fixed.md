# 多來源新聞過濾器 (Multi-source News Filter) 實作計劃

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 StockBoard Android App 中加入「多來源新聞過濾器」功能，遵循 Local-first 架構，所有新聞由 WorkManager 背景抓取並存入 Room，UI 層透過 StateFlow + Room Flow 進行多維度動態過濾。

**Architecture:** NewsEntity 存入 Room `news_cache` 資料表（以 URL 的 MD5 作為主鍵防重複）；`StockUpdateWorker` 批次呼叫 4 個 RSS 來源（Yahoo、CNA、TechNews、Inside）並寫入 DB；`NewsViewModel` 用 `combine` + `flatMapLatest` 將過濾條件與 `NewsDao.getFilteredNews` Flow 綁定；`NewsScreen` 讀取 `StateFlow<NewsUiState>` 渲染 UI，嚴禁在 ViewModel 中發送任何 HTTP 請求。

**Tech Stack:** Kotlin、Jetpack Compose、Room 2.6.0、WorkManager 2.8.1、Retrofit 2.9.0 + OkHttp 4.11.0、Coil 2.5.0（新增）、AndroidX Browser 1.7.0（新增）、Android 內建 XmlPullParser（RSS 解析）

---

## 檔案結構

| 操作 | 檔案 | 職責 |
|------|------|------|
| 修改 | `app/build.gradle.kts` | 新增 Coil、Browser、material-icons-extended 依賴 |
| 新建 | `app/src/main/java/com/stockboard/data/db/NewsEntity.kt` | Room Entity，欄位定義 |
| 新建 | `app/src/main/java/com/stockboard/data/db/NewsDao.kt` | DAO 介面，含進階過濾查詢 |
| 修改 | `app/src/main/java/com/stockboard/data/db/AppDatabase.kt` | 升版至 v2，加入 Migration |
| 新建 | `app/src/main/java/com/stockboard/data/network/RssNewsService.kt` | Retrofit 介面，回傳原始 `ResponseBody` |
| 新建 | `app/src/main/java/com/stockboard/data/network/NewsRssParser.kt` | RSS / Atom XML 解析，支援 `pubDate` 與 ISO 8601 |
| 修改 | `app/src/main/java/com/stockboard/data/network/ApiClient.kt` | 新增 `rssNewsService` 懶加載實例 |
| 修改 | `app/src/main/java/com/stockboard/worker/StockUpdateWorker.kt` | 加入 Step 4：批次抓取 4 個 RSS 來源並寫入 Room |
| 新建 | `app/src/main/java/com/stockboard/viewmodel/NewsViewModel.kt` | NewsUiState + 三個 Filter StateFlow + combine |
| 新建 | `app/src/main/java/com/stockboard/ui/news/NewsScreen.kt` | Compose UI：搜尋框 + FilterChip + Slider + LazyColumn |
| 修改 | `app/src/main/java/com/stockboard/ui/nav/AppNavGraph.kt` | 加入 `news` 路由與底部導覽列項目 |

---

## Task 1：新增依賴項

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1：確認專案使用的 Compose BOM 版本**

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -E "compose-(ui|material3|bom)"
```
Expected: 看到 `compose-bom:2024.02.00`（與 `build.gradle.kts` 中宣告一致）

- [ ] **Step 2：在 `dependencies` 區塊末尾加入 Coil、Browser 與 material-icons-extended**

在 `app/build.gradle.kts` 的 `// 廣告: Google AdMob SDK` 那行之後加入：

```kotlin
    // 圖片載入: Coil（新聞卡片封面圖）
    implementation("io.coil-kt:coil-compose:2.5.0")

    // 瀏覽器: Chrome Custom Tabs（點擊新聞開啟）
    implementation("androidx.browser:browser:1.7.0")

    // Material Icons Extended（包含 Icons.Filled.Newspaper，core 套件不含此圖示）
    // 不指定版本，由 Compose BOM 統一管理，避免與 compose-ui / material3 版本衝突
    implementation("androidx.compose.material:material-icons-extended")
```

- [ ] **Step 3：同步 Gradle，確認無版本衝突**

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -E "coil|browser|icons-extended"
```
Expected: 看到 `coil-compose:2.5.0`、`browser:1.7.0`，以及 `material-icons-extended` 版本號與 BOM 指定的 `compose-ui` 版本一致

- [ ] **Step 4：Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: add Coil, AndroidX Browser, and material-icons-extended for news feature"
```

---

## Task 2：NewsEntity + NewsDao

**Files:**
- Create: `app/src/main/java/com/stockboard/data/db/NewsEntity.kt`
- Create: `app/src/main/java/com/stockboard/data/db/NewsDao.kt`

- [ ] **Step 1：建立 `NewsEntity.kt`**

```kotlin
package com.stockboard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_cache")
data class NewsEntity(
    /** URL 的 MD5 雜湊值，確保同一則新聞不重複寫入 */
    @PrimaryKey
    val id: String,

    val title: String,

    /** 來源識別字："Yahoo" | "CNA" | "TechNews" | "Inside" */
    val source: String,

    /** Unix Timestamp（毫秒） */
    @ColumnInfo(name = "publish_time")
    val publishTime: Long,

    val url: String,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?
)
```

- [ ] **Step 2：建立 `NewsDao.kt`**

```kotlin
package com.stockboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(news: List<NewsEntity>)

    /**
     * 多維度過濾查詢。
     * - query：標題關鍵字（空字串則略過條件）；呼叫方須先跳脫 /、%、_ 三個特殊字元
     * - sources：來源清單（至少一個，呼叫方必須確保非空）
     * - minPublishTime：最早發布時間（Unix ms）
     *
     * ESCAPE '/' 使用正斜線作為跳脫符號，避免 Kotlin raw string 中反斜線與 SQLite
     * 單引號結合造成解析歧義；/ 本身不是 LIKE 特殊字元，無雙重跳脫問題。
     */
    @Query("""
        SELECT * FROM news_cache
        WHERE (:query = '' OR title LIKE '%' || :query || '%' ESCAPE '/')
        AND source IN (:sources)
        AND publish_time >= :minPublishTime
        ORDER BY publish_time DESC
    """)
    fun getFilteredNews(
        query: String,
        sources: List<String>,
        minPublishTime: Long
    ): Flow<List<NewsEntity>>

    /** 刪除超過 48 小時的舊新聞，由 Worker 呼叫以防止 DB 無限增長 */
    @Query("DELETE FROM news_cache WHERE publish_time < :cutoffTime")
    suspend fun deleteOldNews(cutoffTime: Long)
}
```

- [ ] **Step 3：Commit**

```bash
git add app/src/main/java/com/stockboard/data/db/NewsEntity.kt \
        app/src/main/java/com/stockboard/data/db/NewsDao.kt
git commit -m "feat: add NewsEntity and NewsDao for news_cache table"
```

---

## Task 3：AppDatabase 升版至 v2

**Files:**
- Modify: `app/src/main/java/com/stockboard/data/db/AppDatabase.kt`

- [ ] **Step 1：修改 `AppDatabase.kt`**

完整替換為：

```kotlin
package com.stockboard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WatchlistItem::class, StockMeta::class, QuoteCache::class, NewsEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun stockMetaDao(): StockMetaDao
    abstract fun quoteCacheDao(): QuoteCacheDao
    abstract fun newsDao(): NewsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1 → v2：新增 news_cache 資料表 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS news_cache (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        source TEXT NOT NULL,
                        publish_time INTEGER NOT NULL,
                        url TEXT NOT NULL,
                        image_url TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stockboard_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

- [ ] **Step 2：確認 Build 無錯誤**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3：Commit**

```bash
git add app/src/main/java/com/stockboard/data/db/AppDatabase.kt
git commit -m "feat: upgrade AppDatabase to v2, add news_cache table with migration"
```

---

## Task 4：RSS 網路層（RssNewsService + NewsRssParser + ApiClient）

**Files:**
- Create: `app/src/main/java/com/stockboard/data/network/RssNewsService.kt`
- Create: `app/src/main/java/com/stockboard/data/network/NewsRssParser.kt`
- Modify: `app/src/main/java/com/stockboard/data/network/ApiClient.kt`

- [ ] **Step 1：建立 `RssNewsService.kt`**

```kotlin
package com.stockboard.data.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * 以 @Url 動態指定完整 RSS URL，不依賴固定 baseUrl。
 * Retrofit 原生支援回傳 ResponseBody，無需額外 Converter。
 */
interface RssNewsService {
    @GET
    suspend fun fetchRss(@Url url: String): ResponseBody
}
```

- [ ] **Step 2：建立 `NewsRssParser.kt`**

```kotlin
package com.stockboard.data.network

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Locale

/**
 * 支援 RSS 2.0（pubDate：RFC 2822）與 Atom（published：ISO 8601）格式。
 * 命名空間處理關閉，透過 substringAfterLast(":") 統一取 local name。
 */
object NewsRssParser {

    data class ParsedItem(
        val title: String,
        val link: String,
        val publishTimeMs: Long,
        val imageUrl: String?
    )

    fun parse(xml: String): List<ParsedItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.reader())

        val items = mutableListOf<ParsedItem>()
        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var imageUrl: String? = null

        // 標準 Android 官方做法：用 TEXT 事件累積文字，完全不呼叫 nextText()，
        // 無任何 parser 推進狀態不一致的風險。
        val currentText = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            // 去除命名空間前綴，例如 "media:content" → "content"
            val tag = parser.name?.substringAfterLast(":") ?: ""

            when (event) {
                XmlPullParser.START_TAG -> {
                    currentText.clear()
                    when (tag) {
                        "item", "entry" -> {
                            inItem = true
                            title = ""; link = ""; pubDate = ""; imageUrl = null
                        }
                        // Atom <link href="..."/>：屬性在 START_TAG 時讀取
                        "link" -> if (inItem) {
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrBlank()) link = href
                        }
                        // <media:content url="..."/> 或 <media:thumbnail url="..."/>
                        "content", "thumbnail" -> if (inItem && imageUrl == null) {
                            imageUrl = parser.getAttributeValue(null, "url")
                        }
                        // <enclosure url="..." type="image/jpeg"/>
                        "enclosure" -> if (inItem && imageUrl == null) {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (type.startsWith("image")) {
                                imageUrl = parser.getAttributeValue(null, "url")
                            }
                        }
                    }
                }
                // 累積文字節點（CDATA 與一般文字均透過此事件送出）
                XmlPullParser.TEXT -> currentText.append(parser.text ?: "")
                XmlPullParser.END_TAG -> when (tag) {
                    "title" -> if (inItem && title.isEmpty()) {
                        title = currentText.toString().trim()
                    }
                    // RSS <link> 文字節點；若已由 href 屬性填入則跳過
                    "link" -> if (inItem && link.isEmpty()) {
                        link = currentText.toString().trim()
                    }
                    "pubDate", "published", "updated" -> if (inItem && pubDate.isEmpty()) {
                        pubDate = currentText.toString().trim()
                    }
                    "item", "entry" -> {
                        if (inItem && title.isNotBlank() && link.isNotBlank()) {
                            items.add(ParsedItem(title, link, parseDate(pubDate), imageUrl))
                        }
                        inItem = false
                    }
                }
            }
            event = parser.next()
        }
        return items
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            if (dateStr.contains("T")) {
                // ISO 8601（Atom）：需 API 26+，StockBoard minSdk = 26
                OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
            } else {
                // RFC 2822（RSS）
                SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                    .parse(dateStr)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
```

- [ ] **Step 3：修改 `ApiClient.kt`，加入 `rssNewsService`**

在最後一個服務 `twelveDataService` 的 `}` 後，於 `}` （object 結尾）之前加入：

```kotlin
    val rssNewsService: RssNewsService by lazy {
        // 加入 User-Agent 避免部分 RSS 來源拒絕爬蟲
        val rssClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (compatible; StockBoard/1.0)")
                        .build()
                )
            }
            .build()
        // baseUrl 不重要，@Url 會覆寫完整 URL
        Retrofit.Builder()
            .baseUrl("https://www.example.com/")
            .client(rssClient)
            .build()
            .create(RssNewsService::class.java)
    }
```

- [ ] **Step 4：確認 Build 無錯誤**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5：Commit**

```bash
git add app/src/main/java/com/stockboard/data/network/RssNewsService.kt \
        app/src/main/java/com/stockboard/data/network/NewsRssParser.kt \
        app/src/main/java/com/stockboard/data/network/ApiClient.kt
git commit -m "feat: add RssNewsService, NewsRssParser, and ApiClient rssNewsService"
```

---

## Task 5：StockUpdateWorker 擴充新聞抓取

**Files:**
- Modify: `app/src/main/java/com/stockboard/worker/StockUpdateWorker.kt`

- [ ] **Step 1：在現有 `doWork()` 中加入 Step 4 新聞抓取邏輯**

在 `StockUpdateWorker.kt` 的 `companion object` 中補充常數，並在 `doWork()` 的 `Result.success()` 之前加入新聞抓取呼叫：

完整修改後的 `StockUpdateWorker.kt`：

```kotlin
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
     * [Fix] 從 companion object 移至類別層級，避免 extension function
     * 在 companion object 外部無法被解析的編譯錯誤。
     */
    private fun String.toMd5(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(toByteArray())
        // 使用 and 0xFF 遮罩，將 Kotlin Signed Byte 轉為正整數後格式化，
        // 避免負值（如 -1）被展開為 ffffffff 而破壞 32 字元的 MD5 格式
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
                // [Fix] 以 .use {} 包裹 ResponseBody：
                // 1. ResponseBody.string() 是破壞性單次讀取，讀完即關閉。
                // 2. .use {} 確保無論成功或例外都呼叫 close()，
                //    防止底層 BufferedSource 洩漏、OkHttp 連線池耗盡。
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
```

- [ ] **Step 2：確認 Build 無錯誤**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3：Commit**

```bash
git add app/src/main/java/com/stockboard/worker/StockUpdateWorker.kt
git commit -m "feat: extend StockUpdateWorker to fetch and cache multi-source news RSS"
```

---

## Task 6：NewsViewModel

**Files:**
- Create: `app/src/main/java/com/stockboard/viewmodel/NewsViewModel.kt`

- [ ] **Step 1：建立 `NewsViewModel.kt`**

```kotlin
package com.stockboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockboard.data.db.AppDatabase
import com.stockboard.data.db.NewsEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class NewsUiState(
    val newsList: List<NewsEntity> = emptyList()
)

/** 所有可過濾的新聞來源，順序對應 UI FilterChip 排列 */
val ALL_NEWS_SOURCES = listOf("Yahoo", "CNA", "TechNews", "Inside")

/** 時間範圍選項（小時），對應 Slider 的 0、1、2 三個刻度 */
val TIME_RANGE_OPTIONS = listOf(12, 24, 48)

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val newsDao = AppDatabase.getDatabase(application).newsDao()

    /** 搜尋關鍵字 */
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    /** 已選取的來源集合（預設全選） */
    private val _selectedSources = MutableStateFlow(ALL_NEWS_SOURCES.toSet())
    val selectedSources: StateFlow<Set<String>> = _selectedSources.asStateFlow()

    /** 時間範圍（小時）：12 / 24 / 48 */
    private val _timeRangeHours = MutableStateFlow(24)
    val timeRangeHours: StateFlow<Int> = _timeRangeHours.asStateFlow()

    /**
     * 每 5 分鐘 emit 一次，驅動 minPublishTime 重新計算。
     * 若不加此 ticker，minPublishTime 只在使用者動過濾器時才重算，
     * 導致「48 小時視窗」的最舊邊界隨時間凍結（使用者體驗 Bug）。
     */
    private val _ticker = flow {
        while (true) {
            emit(Unit)
            delay(5 * 60 * 1000L)
        }
    }

    /**
     * 單一出口 StateFlow，由三個過濾條件 + ticker combine 後 flatMapLatest 到 Room DAO Flow。
     * 嚴禁在此 ViewModel 加入任何 HTTP 請求。
     */
    val uiState: StateFlow<NewsUiState> = combine(
        _searchKeyword,
        _selectedSources,
        _timeRangeHours,
        _ticker
    ) { keyword, sources, hours, _ ->
        Triple(keyword, sources, hours)
    }.flatMapLatest { (keyword, sources, hours) ->
        if (sources.isEmpty()) {
            // 來源全部取消選取時，直接回傳空清單，避免 Room IN () 錯誤
            flowOf(emptyList())
        } else {
            val minPublishTime = System.currentTimeMillis() - hours * 3600L * 1000L
            // 跳脫 SQLite LIKE 萬用字元；使用 / 作為 escape char（對應 DAO 的 ESCAPE '/'）
            // / 本身不是 LIKE 特殊字元，不需要先跳脫跳脫符號，不存在雙重跳脫問題
            val escapedKeyword = keyword
                .replace("/", "//")
                .replace("%", "/%")
                .replace("_", "/_")
            newsDao.getFilteredNews(
                query = escapedKeyword,
                sources = sources.toList(),
                minPublishTime = minPublishTime
            )
        }
    }.map { list ->
        NewsUiState(newsList = list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NewsUiState()
    )

    fun onSearchKeywordChange(keyword: String) {
        _searchKeyword.value = keyword
    }

    /** 切換單一來源的過濾狀態 */
    fun onSourceToggle(source: String) {
        val current = _selectedSources.value
        _selectedSources.value = if (source in current) {
            current - source
        } else {
            current + source
        }
    }

    /** Slider 索引（0=12h、1=24h、2=48h）→ 對應小時值 */
    fun onTimeRangeSliderChange(sliderIndex: Int) {
        _timeRangeHours.value = TIME_RANGE_OPTIONS.getOrElse(sliderIndex) { 24 }
    }
}
```

- [ ] **Step 2：確認 Build 無錯誤**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3：Commit**

```bash
git add app/src/main/java/com/stockboard/viewmodel/NewsViewModel.kt
git commit -m "feat: add NewsViewModel with multi-source filter StateFlow combining"
```

---

## Task 7：NewsScreen（Compose UI）

**Files:**
- Create: `app/src/main/java/com/stockboard/ui/news/NewsScreen.kt`

- [ ] **Step 1：建立 `NewsScreen.kt`**

```kotlin
package com.stockboard.ui.news

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.stockboard.data.db.NewsEntity
import com.stockboard.ui.theme.CardDark
import com.stockboard.ui.theme.ColorUp
import com.stockboard.ui.theme.TextSecondary
import com.stockboard.viewmodel.ALL_NEWS_SOURCES
import com.stockboard.viewmodel.TIME_RANGE_OPTIONS
import com.stockboard.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NewsScreen(newsViewModel: NewsViewModel = viewModel()) {
    val uiState by newsViewModel.uiState.collectAsState()
    val searchKeyword by newsViewModel.searchKeyword.collectAsState()
    val selectedSources by newsViewModel.selectedSources.collectAsState()
    val timeRangeHours by newsViewModel.timeRangeHours.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 頂部控制面板
        NewsFilterPanel(
            searchKeyword = searchKeyword,
            selectedSources = selectedSources,
            timeRangeHours = timeRangeHours,
            onKeywordChange = newsViewModel::onSearchKeywordChange,
            onSourceToggle = newsViewModel::onSourceToggle,
            onTimeRangeChange = newsViewModel::onTimeRangeSliderChange
        )

        // 新聞列表
        if (uiState.newsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "目前無新聞，請等待背景同步",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.newsList,
                    key = { it.id }
                ) { news ->
                    NewsCard(news = news)
                }
            }
        }
    }
}

@Composable
private fun NewsFilterPanel(
    searchKeyword: String,
    selectedSources: Set<String>,
    timeRangeHours: Int,
    onKeywordChange: (String) -> Unit,
    onSourceToggle: (String) -> Unit,
    onTimeRangeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 關鍵字搜尋框
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = onKeywordChange,
            label = { Text("搜尋新聞標題") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 來源過濾 FilterChip（橫向捲動）
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ALL_NEWS_SOURCES) { source ->
                FilterChip(
                    selected = source in selectedSources,
                    onClick = { onSourceToggle(source) },
                    label = { Text(source) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorUp.copy(alpha = 0.2f),
                        selectedLabelColor = ColorUp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 時間範圍拉桿
        val sliderIndex = TIME_RANGE_OPTIONS.indexOf(timeRangeHours).coerceAtLeast(0).toFloat()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "時間範圍：",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = sliderIndex,
                onValueChange = { onTimeRangeChange(it.roundToInt()) },
                valueRange = 0f..2f,
                steps = 1,   // 0、1、2 三個刻度
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${timeRangeHours} 小時",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(52.dp)
            )
        }
    }
}

@Composable
private fun NewsCard(news: NewsEntity) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                CustomTabsIntent
                    .Builder()
                    .build()
                    .launchUrl(context, Uri.parse(news.url))
            },
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 新聞封面圖（Coil 非同步載入）
            if (!news.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(news.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "新聞圖片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 標題 + 來源 + 時間
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = news.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = news.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorUp
                    )
                    Text(
                        text = "  ·  ${formatPublishTime(news.publishTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private fun formatPublishTime(timestampMs: Long): String {
    // maxOf 防護：手機與伺服器時鐘存在微小誤差時，elapsed 可能為負，
    // 不加保護會渲染出「-2 分鐘前」等異常文字
    val elapsed = maxOf(0L, System.currentTimeMillis() - timestampMs)
    return when {
        elapsed < 3600_000L -> "${elapsed / 60_000} 分鐘前"
        elapsed < 86400_000L -> "${elapsed / 3600_000} 小時前"
        else -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestampMs))
    }
}
```

- [ ] **Step 2：確認 Build 無錯誤**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3：Commit**

```bash
git add app/src/main/java/com/stockboard/ui/news/NewsScreen.kt
git commit -m "feat: add NewsScreen with search, FilterChip, Slider, and Coil news cards"
```

---

## Task 8：導航整合（AppNavGraph）

**Files:**
- Modify: `app/src/main/java/com/stockboard/ui/nav/AppNavGraph.kt`

- [ ] **Step 1：修改 `AppNavGraph.kt`，加入新聞頁籤與路由**

完整替換為：

```kotlin
package com.stockboard.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stockboard.ui.ads.BannerAdComposable
import com.stockboard.ui.home.HomeScreen
import com.stockboard.ui.manage.ManageScreen
import com.stockboard.ui.manage.WatchlistScreen
import com.stockboard.ui.news.NewsScreen
import com.stockboard.ui.theme.CardDark
import com.stockboard.ui.theme.ColorUp
import com.stockboard.ui.theme.TextSecondary

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            Column {
                BannerAdComposable(adUnitId = com.stockboard.BuildConfig.ADMOB_BANNER_ID)

                NavigationBar(containerColor = CardDark) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "首頁") },
                        label = { Text("首頁") },
                        selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorUp,
                            selectedTextColor = ColorUp,
                            indicatorColor = ColorUp.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Newspaper, contentDescription = "新聞") },
                        label = { Text("財經新聞") },
                        selected = currentDestination?.hierarchy?.any { it.route == "news" } == true,
                        onClick = {
                            navController.navigate("news") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorUp,
                            selectedTextColor = ColorUp,
                            indicatorColor = ColorUp.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Add, contentDescription = "新增") },
                        label = { Text("新增自選") },
                        selected = currentDestination?.hierarchy?.any { it.route == "manage" } == true,
                        onClick = {
                            navController.navigate("manage") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorUp,
                            selectedTextColor = ColorUp,
                            indicatorColor = ColorUp.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Delete, contentDescription = "刪除") },
                        label = { Text("管理清單") },
                        selected = currentDestination?.hierarchy?.any { it.route == "watchlist" } == true,
                        onClick = {
                            navController.navigate("watchlist") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorUp,
                            selectedTextColor = ColorUp,
                            indicatorColor = ColorUp.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("news") { NewsScreen() }
            composable("manage") { ManageScreen() }
            composable("watchlist") { WatchlistScreen() }
        }
    }
}
```

- [ ] **Step 2：確認完整 Build 成功**

```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3：Commit**

```bash
git add app/src/main/java/com/stockboard/ui/nav/AppNavGraph.kt
git commit -m "feat: add news route and bottom nav item to AppNavGraph"
```

---

## 驗收測試

| 測試項目 | 步驟 | 預期結果 |
|---------|------|---------|
| DB 升版 | 安裝在已有 v1 DB 的裝置上執行 App | 無 crash，舊資料保留 |
| 新聞同步 | 等待 15 分鐘 WorkManager 觸發，或手動呼叫 `adb shell am broadcast -a androidx.work.impl.background.systemalarm.RescheduleReceiver` | Logcat 顯示「新聞已寫入 Yahoo：N 則」 |
| 過濾功能 | 進入「財經新聞」頁，輸入關鍵字 / 切換來源 / 拖動時間範圍拉桿 | 列表即時更新，無網路呼叫發生 |
| 防重複 | 連續觸發兩次 Worker | `news_cache` 記錄數不增加（`OnConflictStrategy.IGNORE` 生效） |
| 圖片載入 | 開啟新聞頁，觀察有圖的卡片 | 圖片非同步顯示，無 OOM 崩潰 |
| 點擊開啟 | 點選任意新聞卡片 | Chrome Custom Tabs 開啟對應 URL |
| 禁止前景 HTTP | 使用 Network Profiler 或 StrictMode 監控 ViewModel | `NewsViewModel` 無任何網路流量 |

---

## 嚴格限制檢查清單

- [x] `NewsViewModel` 不含任何 Retrofit 呼叫，僅依賴 `newsDao.getFilteredNews` Flow
- [x] `NewsEntity.id` = URL MD5 雜湊，`@Insert(onConflict = IGNORE)` 防重複
- [x] 圖片透過 Coil `AsyncImage` 載入，無 Base64/ByteArray 存入 DB
- [x] UI 標籤使用台灣繁體中文（首頁、財經新聞、新增自選、管理清單）
- [x] `deleteOldNews` 在每次 Worker 執行時清除 48 小時前舊新聞，防止 DB 無限增長
