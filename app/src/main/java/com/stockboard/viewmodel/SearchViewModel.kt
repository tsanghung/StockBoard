package com.stockboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockboard.data.db.StockMeta
import com.stockboard.data.db.WatchlistDao
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

// 規格書 3-3：允許的美股類型
private val ALLOWED_US_TYPES = setOf("Common Stock", "ETP")

data class SearchUiState(
    val mode: SearchMode = SearchMode.TAIWAN,
    val query: String = "",
    val twResults: List<StockMeta> = emptyList(),
    val usResults: List<FinnhubSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val addedMessage: String? = null,
    val errorMessage: String? = null
)

class SearchViewModel(
    private val repository: StockMetaRepository,
    private val watchlistDao: WatchlistDao
) : ViewModel() {

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
     * Task 3-3 核心：美股搜尋
     * 呼叫 Finnhub GET /api/v1/search?q={query}
     * 過濾 type = "Common Stock" 或 "ETP"
     */
    private fun searchUS(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // TODO: 正式版從 local.properties 讀取 API Key
                val response = ApiClient.finnhubService.searchSymbol(
                    query = query,
                    token = "demo_token"
                )
                val filtered = response.result
                    .filter { it.type in ALLOWED_US_TYPES }
                    .take(20)
                _uiState.value = _uiState.value.copy(usResults = filtered, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "美股搜尋失敗：${e.message}"
                )
            }
        }
    }

    /** 新增台股至 watchlist（market = TAIWAN） */
    fun addTaiwanStockToWatchlist(stock: StockMeta) {
        viewModelScope.launch {
            val item = WatchlistItem(
                symbol = stock.symbol,
                name = stock.name,
                market = MarketType.TAIWAN,
                sortOrder = 0,
                isFixed = false
            )
            watchlistDao.insertItem(item)
            _uiState.value = _uiState.value.copy(addedMessage = "${stock.name}（${stock.symbol}）已加入台股自選")
        }
    }

    /** Task 3-3：新增美股至 watchlist（market = US） */
    fun addUsStockToWatchlist(stock: FinnhubSearchResult) {
        viewModelScope.launch {
            val item = WatchlistItem(
                symbol = stock.symbol,
                name = stock.description,
                market = MarketType.US,
                sortOrder = 0,
                isFixed = false
            )
            watchlistDao.insertItem(item)
            _uiState.value = _uiState.value.copy(addedMessage = "${stock.description}（${stock.symbol}）已加入美股自選")
        }
    }

    fun clearAddedMessage() {
        _uiState.value = _uiState.value.copy(addedMessage = null)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
