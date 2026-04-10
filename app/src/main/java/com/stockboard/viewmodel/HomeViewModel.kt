package com.stockboard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockboard.data.db.AppDatabase
import com.stockboard.data.model.TwseMisItem
import com.stockboard.data.network.ApiClient
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

data class HomeUiState(
    val isLoading: Boolean = false,
    val twIndices: List<UsIndexUiModel> = emptyList(),
    val usIndices: List<UsIndexUiModel> = emptyList(),
    val errorMessage: String? = null,
    val lastUpdated: String = "尚未更新",
    val rateLimitMessage: String? = null
)

class HomeViewModel(
    private val appDatabase: AppDatabase? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()


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
        _uiState.value = _uiState.value.copy(lastUpdated = timeFormatter.format(Date()))
    }

    /**
     * 台股指數：上市 / 上櫃 → TWSE MIS 官方即時 API
     */
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

    private fun parseMisItem(item: TwseMisItem?, symbol: String, name: String): UsIndexUiModel {
        val price = item?.z?.toDoubleOrNull()   // "-" 或空字串會回傳 null
        val prev  = item?.y?.toDoubleOrNull()
        val change = if (price != null && prev != null) price - prev else null
        val pct    = if (change != null && prev != null && prev != 0.0) change / prev * 100 else null
        return UsIndexUiModel(symbol, name, price, change, pct)
    }

    /** 美股真實指數（v8 Chart API on query2，繞過 query1 的 401 限制） */
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
                    UsIndexUiModel(symbol, name)  // 單一 symbol 失敗不影響其他
                }
            }
        }.awaitAll()
        _uiState.value = _uiState.value.copy(usIndices = results)
    }

    private fun getSecondsUntilNextRefresh(): Long {
        val elapsed = System.currentTimeMillis() - (System.currentTimeMillis() - RateLimiter.MINIMUM_INTERVAL_MS)
        return maxOf(0L, (RateLimiter.MINIMUM_INTERVAL_MS - elapsed) / 1000L)
    }
}
