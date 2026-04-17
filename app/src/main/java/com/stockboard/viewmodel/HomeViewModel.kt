package com.stockboard.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockboard.data.db.AppDatabase
import com.stockboard.data.db.WatchlistItem
import com.stockboard.data.network.ApiClient
import com.stockboard.util.MarketType
import com.stockboard.util.RateLimiter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UsIndexUiModel(
    val symbol: String,
    val shortName: String,
    val price: Double? = null,
    val change: Double? = null,
    val changePercent: Double? = null,
    val url: String? = null
)

data class StockQuoteUiModel(
    val symbol: String,
    val name: String,
    val market: MarketType,
    val badgeText: String,
    val price: Double? = null,
    val change: Double? = null,
    val changePercent: Double? = null,
    val sourceExchange: String? = null
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val twIndices: List<UsIndexUiModel> = emptyList(),
    val usIndices: List<UsIndexUiModel> = emptyList(),
    val twWatchlist: List<StockQuoteUiModel> = emptyList(),
    val usWatchlist: List<StockQuoteUiModel> = emptyList(),
    val errorMessage: String? = null,
    val lastUpdated: String = "尚未更新",
    val rateLimitMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _watchlistItems = MutableStateFlow<List<WatchlistItem>>(emptyList())

    // Yahoo Finance 美股真實指數 symbol 及對應的 Google Finance 網址
    private val usMarketSymbols = listOf(
        Triple("^DJI",  "道瓊工業",   "https://www.google.com/finance/quote/.DJI:INDEXDJX?hl=zh-TW&window=YTD"),
        Triple("^IXIC", "NASDAQ",     "https://www.google.com/finance/quote/.IXIC:INDEXNASDAQ?hl=zh-TW&window=YTD"),
        Triple("^GSPC", "S&P 500",   "https://www.google.com/finance/quote/.INX:INDEXSP?hl=zh-TW&window=YTD"),
        Triple("^SOX",  "費半 SOX",   "https://www.google.com/finance/quote/SOX:INDEXNASDAQ?hl=zh-TW&window=YTD"),
        Triple("TSM",   "台積電 ADR", "https://www.google.com/finance/quote/TSM:NYSE?hl=zh-TW&window=YTD")
    )

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MS = 5 * 60 * 1000L
        private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    init {
        _uiState.value = _uiState.value.copy(
            usIndices = usMarketSymbols.map { UsIndexUiModel(it.first, it.second, url = it.third) }
        )
        // 持續訂閱自選股清單，清單變動時觸發重新報價
        viewModelScope.launch {
            db.watchlistDao().getAllWatchlist().collect { items ->
                _watchlistItems.value = items
                fetchWatchlistQuotes(items)
            }
        }
        viewModelScope.launch {
            RateLimiter.canRefresh()
            doRefresh()
            startAutoRefresh()
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                performFetch()
            }
        }
    }

    fun manualRefresh() {
        if (RateLimiter.canRefresh()) {
            doRefresh()
        } else {
            val remaining = getSecondsUntilNextRefresh()
            _uiState.value = _uiState.value.copy(
                rateLimitMessage = "剛剛已更新，請 ${remaining} 秒後再試"
            )
        }
    }

    fun clearRateLimitMessage() {
        _uiState.value = _uiState.value.copy(rateLimitMessage = null)
    }

    private fun doRefresh() {
        viewModelScope.launch { performFetch() }
    }

    private suspend fun performFetch() {
        fetchTwIndices()
        fetchYahooUsIndices()
        fetchWatchlistQuotes(_watchlistItems.value)
        _uiState.value = _uiState.value.copy(lastUpdated = timeFormatter.format(Date()))
    }

    /** 台股指數：上市 / 上櫃 → TWSE MIS 官方即時 API */
    private suspend fun fetchTwIndices() {
        val (twii, otc) = fetchTwseMisIndices()
        _uiState.value = _uiState.value.copy(twIndices = listOf(twii, otc))
    }

    /** TWSE MIS 抓取上市 + 上櫃即時指數 */
    private suspend fun fetchTwseMisIndices(): Pair<UsIndexUiModel, UsIndexUiModel> {
        return try {
            val response = ApiClient.twseMisService.getIndexInfo()
            val items = response.msgArray ?: emptyList()
            val tseItem = items.find { it.ex == "tse" }
            val otcItem = items.find { it.ex == "otc" }
            Pair(
                parseMisItem(tseItem, "tse_t00", "上市指數"),
                parseMisItem(otcItem, "otc_o00", "上櫃指數")
            )
        } catch (e: Exception) {
            Pair(
                UsIndexUiModel("tse_t00", "上市指數"),
                UsIndexUiModel("otc_o00", "上櫃指數")
            )
        }
    }

    private fun parseMisItem(item: com.stockboard.data.model.TwseMisItem?, symbol: String, name: String): UsIndexUiModel {
        val price = item?.z?.toDoubleOrNull()
        val prev  = item?.y?.toDoubleOrNull()
        val change = if (price != null && prev != null) price - prev else null
        val pct    = if (change != null && prev != null && prev != 0.0) change / prev * 100 else null
        return UsIndexUiModel(symbol, name, price, change, pct)
    }

    /** 美股真實指數（v8 Chart API on query2） */
    private suspend fun fetchYahooUsIndices() = coroutineScope {
        val results = usMarketSymbols.map { (symbol, name, url) ->
            async {
                try {
                    val encoded = symbol.replace("^", "%5E")
                    val response = ApiClient.yahooChartService.getChart(encoded)
                    val meta = response.chart?.result?.firstOrNull()?.meta
                    val price = meta?.regularMarketPrice
                    val prev = meta?.previousClose ?: meta?.chartPreviousClose
                    val change = if (price != null && prev != null) price - prev else null
                    val pct = if (change != null && prev != null && prev != 0.0) change / prev * 100 else null
                    UsIndexUiModel(symbol, name, price, change, pct, url)
                } catch (e: Exception) {
                    UsIndexUiModel(symbol, name, url = url)
                }
            }
        }.awaitAll()
        _uiState.value = _uiState.value.copy(usIndices = results)
    }

    /**
     * 自選股報價：依 market 分流
     * - TAIWAN → TWSE MIS 即時 API（同時嘗試 tse_ 與 otc_ 前綴）
     * - US     → Yahoo Chart API
     */
    private suspend fun fetchWatchlistQuotes(items: List<WatchlistItem>) {
        val twItems = items.filter { it.market == MarketType.TAIWAN }
        val usItems = items.filter { it.market == MarketType.US }

        val twQuotes = fetchTwStockQuotes(twItems)
        val usQuotes = fetchUsStockQuotes(usItems)

        _uiState.value = _uiState.value.copy(
            twWatchlist = twQuotes,
            usWatchlist = usQuotes
        )
    }

    /**
     * 台股個股報價：用 Yahoo Finance v7 API
     * 對每個 symbol 同時送出 .TW 與 .TWO 後綴，取符合的那個
     */
    private suspend fun fetchTwStockQuotes(items: List<WatchlistItem>): List<StockQuoteUiModel> = coroutineScope {
        if (items.isEmpty()) return@coroutineScope emptyList()

        // 雖然 Yahoo 容忍度極高，但維持適當分批（10檔一批）是良好的網路請求實踐
        val chunkedItems = items.chunked(10)

        val deferredResults = chunkedItems.map { chunk ->
            async {
                // 同時產生 .TW (上市) 與 .TWO (上櫃) 的查詢代碼
                val symbols = chunk.flatMap { listOf("${it.symbol}.TW", "${it.symbol}.TWO") }
                    .joinToString(",")

                try {
                    // 使用專案中現成的 Yahoo v7 API
                    val response = ApiClient.yahooFinanceService.getQuotes(symbols)
                    val quoteResults = response.quoteResponse?.result ?: emptyList()

                    chunk.map { watchlistItem ->
                        // 從回傳陣列中，尋找符合 .TW 或 .TWO 的實際報價物件
                        val quote = quoteResults.firstOrNull { 
                            it.symbol == "${watchlistItem.symbol}.TW" || it.symbol == "${watchlistItem.symbol}.TWO" 
                        }

                        val price = quote?.regularMarketPrice
                        val change = quote?.regularMarketChange
                        val pct = quote?.regularMarketChangePercent
                        
                        // 根據 Yahoo 回傳的後綴字元判定上市或上櫃
                        val badge = when {
                            quote?.symbol?.endsWith(".TW") == true -> "上市"
                            quote?.symbol?.endsWith(".TWO") == true -> "上櫃"
                            else -> "台股"
                        }

                        StockQuoteUiModel(
                            symbol = watchlistItem.symbol,
                            name   = watchlistItem.name,
                            market = watchlistItem.market,
                            badgeText = badge,
                            price = price,
                            change = change,
                            changePercent = pct
                        )
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Yahoo Finance API failed for TW stocks", e)
                    chunk.map { StockQuoteUiModel(it.symbol, it.name, it.market, "台股") }
                }
            }
        }

        deferredResults.awaitAll().flatten()
    }

    /** 美股個股報價：Yahoo Chart API */
    private suspend fun fetchUsStockQuotes(items: List<WatchlistItem>): List<StockQuoteUiModel> = coroutineScope {
        items.map { watchlistItem ->
            async {
                try {
                    val response = ApiClient.yahooChartService.getChart(watchlistItem.symbol)
                    val meta = response.chart?.result?.firstOrNull()?.meta
                    val price = meta?.regularMarketPrice
                    val prev  = meta?.previousClose ?: meta?.chartPreviousClose
                    val change = if (price != null && prev != null) price - prev else null
                    val pct    = if (change != null && prev != null && prev != 0.0) change / prev * 100 else null
                    
                    // Yahoo Finance 傳回的 exchangeName (如 NMS 代表 NASDAQ，NYQ 代表 NYSE)
                    val googleExchange = when (meta?.exchangeName?.uppercase()) {
                        "NYQ", "NYSE" -> "NYSE"
                        "NMS", "NASDAQ", "NCM", "NGM" -> "NASDAQ"
                        "BTS", "BATS", "BGM" -> "BATS"
                        "PNK", "OTC", "OTCMKTS" -> "OTCMKTS"
                        "ASE", "AMEX" -> "NYSEAMERICAN"
                        else -> meta?.exchangeName?.uppercase() ?: "NASDAQ"
                    }
                    
                    StockQuoteUiModel(watchlistItem.symbol, watchlistItem.name, watchlistItem.market, "US", price, change, pct, sourceExchange = googleExchange)
                } catch (e: Exception) {
                    StockQuoteUiModel(watchlistItem.symbol, watchlistItem.name, watchlistItem.market, "US", sourceExchange = "NASDAQ")
                }
            }
        }.awaitAll()
    }

    private fun getSecondsUntilNextRefresh(): Long {
        val elapsed = System.currentTimeMillis() - RateLimiter.lastRefreshTime
        return maxOf(0L, (RateLimiter.MINIMUM_INTERVAL_MS - elapsed) / 1000L)
    }
}
