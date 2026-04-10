package com.stockboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockboard.data.db.WatchlistDao
import com.stockboard.data.db.WatchlistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val items: List<WatchlistItem> = emptyList(),
    val pendingDeleteItem: WatchlistItem? = null,   // 等待 AlertDialog 確認的目標
    val deletedMessage: String? = null
)

/**
 * Task 3-4：自選股管理 ViewModel
 * - getAllWatchlist() Flow 持續訂閱 Room
 * - requestDelete() 觸發 AlertDialog 確認
 * - confirmDelete() 實際呼叫 DAO 刪除
 * - cancelDelete() 取消（關閉 Dialog）
 */
class WatchlistViewModel(private val watchlistDao: WatchlistDao) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            watchlistDao.getAllWatchlist().collect { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }
        }
    }

    /** 使用者滑動或長按後，暫存待刪目標並等候 Dialog 確認 */
    fun requestDelete(item: WatchlistItem) {
        _uiState.value = _uiState.value.copy(pendingDeleteItem = item)
    }

    /** AlertDialog「確定」：執行實際刪除 */
    fun confirmDelete() {
        val target = _uiState.value.pendingDeleteItem ?: return
        viewModelScope.launch {
            watchlistDao.deleteItem(target)
            _uiState.value = _uiState.value.copy(
                pendingDeleteItem = null,
                deletedMessage = "${target.name}（${target.symbol}）已從自選股移除"
            )
        }
    }

    /** AlertDialog「取消」：清除暫存，關閉 Dialog */
    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(pendingDeleteItem = null)
    }

    fun clearDeletedMessage() {
        _uiState.value = _uiState.value.copy(deletedMessage = null)
    }
}
