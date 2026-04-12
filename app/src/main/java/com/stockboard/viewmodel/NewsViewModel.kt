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
