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
    val changePercent: Double? = null
)

data class StockQuoteUiModel(
    val symbol: String,
    val name: String,
    val market: MarketType,
    val badgeText: String,
    val price: Double? = null,
    val change: Double? = null,
    val changePercent: Double? = null
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

    // Yahoo Finance 美股真實指數 symbol
    private val usMarketSymbols = listOf(
        "^DJI"  to "道瓊工業",
        "^IXIC" to "NASDAQ",
        "^GSPC" to "S&P 500",
        "^SOX"  to "費半 SOX",
        "TSM"   to "台積電 ADR"
    )

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MS = 5 * 60 * 1000L
        private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    init {
        _uiState.value = _uiState.value.copy(
            usIndices = usMarketSymbols.map { UsIndexUiModel(it.first, it.second) }
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
        val results = usMarketSymbols.map { (symbol, name) ->
            async {
                try {
                    val encoded = symbol.replace("^", "%5E")
                    val response = ApiClient.yahooChartService.getChart(encoded)
                    val meta = response.chart?.result?.firstOrNull()?.meta
                    val price = meta?.regularMarketPrice
                    val prev = meta?.previousClose ?: meta?.chartPreviousClose
                    val change = if (price != null && prev != null) price - prev else null
                    val pct = if (change != null && prev != null && prev != 0.0) change / prev * 100 else null
                    UsIndexUiModel(symbol, name, price, change, pct)
                } catch (e: Exception) {
                    UsIndexUiModel(symbol, name)
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
     * 台股個股報價：用 TWSE MIS getStockInfo
     * 對每個 symbol 同時送出 tse_ 與 otc_ 前綴，取有成交價的那個
     */
    private suspend fun fetchTwStockQuotes(items: List<WatchlistItem>): List<StockQuoteUiModel> {
        if (items.isEmpty()) return emptyList()

        val exChQuery = items.flatMap { listOf("tse_${it.symbol}.tw", "otc_${it.symbol}.tw") }
            .joinToString("|")

        return try {
            val response = ApiClient.twseMisService.getIndexInfo(symbols = exChQuery)
            val misItems = response.msgArray ?: emptyList()

            items.map { watchlistItem ->
                // 優先取有成交價的，否則退而求其次取任一匹配
                val matched = misItems.filter { it.ch == "${watchlistItem.symbol}.tw" }
                val misItem = matched.firstOrNull { it.z?.toDoubleOrNull() != null }
                    ?: matched.firstOrNull()

                val price = misItem?.z?.toDoubleOrNull()
                val prev  = misItem?.y?.toDoubleOrNull()
                val change = if (price != null && prev != null) price - prev else null
                val pct    = if (change != null && prev != null && prev != 0.0) change / prev * 100 else null
                val badge  = when (misItem?.ex) {
                    "tse" -> "上市"
                    "otc" -> "上櫃"
                    else  -> "台股"
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
            Log.e("HomeViewModel", "fetchTwStockQuotes failed", e)
            items.map { StockQuoteUiModel(it.symbol, it.name, it.market, "台股") }
        }
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
                    StockQuoteUiModel(watchlistItem.symbol, watchlistItem.name, watchlistItem.market, "US", price, change, pct)
                } catch (e: Exception) {
                    StockQuoteUiModel(watchlistItem.symbol, watchlistItem.name, watchlistItem.market, "US")
                }
            }
        }.awaitAll()
    }

    private fun getSecondsUntilNextRefresh(): Long {
        val elapsed = System.currentTimeMillis() - RateLimiter.lastRefreshTime
        return maxOf(0L, (RateLimiter.MINIMUM_INTERVAL_MS - elapsed) / 1000L)
    }
}
