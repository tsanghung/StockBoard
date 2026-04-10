package com.stockboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockboard.data.db.AppDatabase
import com.stockboard.data.db.StockMeta
import com.stockboard.data.db.WatchlistItem
import com.stockboard.data.model.FinnhubSearchResult
import com.stockboard.data.network.ApiClient
import com.stockboard.data.repository.StockMetaRepository
import com.stockboard.util.MarketType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 搜尋模式：台股 or 美股
enum class SearchMode { TAIWAN, US }

data class SearchUiState(
    val mode: SearchMode = SearchMode.TAIWAN,
    val query: String = "",
    val twResults: List<StockMeta> = emptyList(),
    val usResults: List<FinnhubSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val addedMessage: String? = null,
    val errorMessage: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val watchlistDao = db.watchlistDao()
    private val repository = StockMetaRepository(db.stockMetaDao())

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // 啟動時觸發台股 24h 快取檢查
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.checkAndRefresh()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    /** 切換搜尋模式（台股 / 美股），切換時清空輸入與結果 */
    fun switchMode(mode: SearchMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            query = "",
            twResults = emptyList(),
            usResults = emptyList()
        )
    }

    /** 通用搜尋入口：依目前 mode 分流 */
    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(twResults = emptyList(), usResults = emptyList())
            return
        }
        when (_uiState.value.mode) {
            SearchMode.TAIWAN -> searchTaiwan(query)
            SearchMode.US -> searchUS(query)
        }
    }

    /** 台股：Room LIKE 查詢 */
    private fun searchTaiwan(query: String) {
        viewModelScope.launch {
            val results = repository.searchStocks(query)
            _uiState.value = _uiState.value.copy(twResults = results)
        }
    }

    /**
     * 美股搜尋：呼叫 YahooChartService 驗證 symbol 是否存在
     * Chart API 查無代號會拋出 HTTP 404，作為驗證機制
     * 查獲後包裝為 FinnhubSearchResult 以維持 UI 一致性
     */
    private fun searchUS(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val symbolParam = query.uppercase()
                val response = ApiClient.yahooChartService.getChart(symbolParam)

                val meta = response.chart?.result?.firstOrNull()?.meta
                if (meta != null && meta.regularMarketPrice != null) {
                    val result = FinnhubSearchResult(
                        description = meta.symbol ?: symbolParam,
                        displaySymbol = meta.symbol ?: symbolParam,
                        symbol = meta.symbol ?: symbolParam,
                        type = "Common Stock"
                    )
                    _uiState.value = _uiState.value.copy(usResults = listOf(result), isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        usResults = emptyList(),
                        isLoading = false,
                        errorMessage = "查無此美股代號"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    usResults = emptyList(),
                    isLoading = false,
                    errorMessage = "查無此代號或網路異常"
                )
            }
        }
    }

    /** 新增台股至 watchlist，加入前檢查重複並使用正確 sort_order */
    fun addTaiwanStockToWatchlist(stock: StockMeta) {
        viewModelScope.launch {
            val existsCount = watchlistDao.countBySymbol(stock.symbol, MarketType.TAIWAN)
            if (existsCount > 0) {
                _uiState.value = _uiState.value.copy(errorMessage = "${stock.symbol} 已存在於自選清單中")
                return@launch
            }
            val currentMaxOrder = watchlistDao.getMaxSortOrder(MarketType.TAIWAN) ?: -1
            watchlistDao.insertItem(
                WatchlistItem(
                    symbol = stock.symbol,
                    name = stock.name,
                    market = MarketType.TAIWAN,
                    sortOrder = currentMaxOrder + 1,
                    isFixed = false
                )
            )
            _uiState.value = _uiState.value.copy(addedMessage = "${stock.name} 已加入台股自選")
        }
    }

    /** 新增美股至 watchlist，加入前檢查重複並使用正確 sort_order */
    fun addUsStockToWatchlist(stock: FinnhubSearchResult) {
        viewModelScope.launch {
            val existsCount = watchlistDao.countBySymbol(stock.symbol, MarketType.US)
            if (existsCount > 0) {
                _uiState.value = _uiState.value.copy(errorMessage = "${stock.symbol} 已存在於自選清單中")
                return@launch
            }
            val currentMaxOrder = watchlistDao.getMaxSortOrder(MarketType.US) ?: -1
            watchlistDao.insertItem(
                WatchlistItem(
                    symbol = stock.symbol,
                    name = stock.description,
                    market = MarketType.US,
                    sortOrder = currentMaxOrder + 1,
                    isFixed = false
                )
            )
            _uiState.value = _uiState.value.copy(addedMessage = "${stock.description} 已加入美股自選")
        }
    }

    fun clearAddedMessage() {
        _uiState.value = _uiState.value.copy(addedMessage = null)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
