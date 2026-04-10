package com.stockboard.data.repository

import com.stockboard.data.db.StockMeta
import com.stockboard.data.db.StockMetaDao
import com.stockboard.data.network.ApiClient
import com.stockboard.util.MarketType

class StockMetaRepository(private val stockMetaDao: StockMetaDao) {

    companion object {
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 小時
    }

    /**
     * Task 3-2 核心邏輯：24 小時快取策略
     * 1. 查詢 stock_meta 是否有資料且未過期
     * 2. 若快取過期（超過 24h）或從未下載 → 重新呼叫 TWSE + TPEX OpenAPI
     * 3. 備妥後直接回傳，無需等候（前端持有 Flow 在更新時自動刷新）
     */
    suspend fun checkAndRefresh() {
        val count = stockMetaDao.getCount()
        val lastUpdated = stockMetaDao.getLastUpdated()
        val now = System.currentTimeMillis()

        val needRefresh = count == 0 || lastUpdated == null || (now - lastUpdated) > CACHE_DURATION_MS

        if (needRefresh) {
            downloadAndCache(now)
        }
    }

    private suspend fun downloadAndCache(timestamp: Long) {
        try {
            val result = mutableListOf<StockMeta>()

            // 下載上市股票（TWSE）
            val tseStocks = ApiClient.twseService.getTseStocks()
            tseStocks.forEach { item ->
                val symbol = item.symbol.trim()
                val name = item.name.trim()
                if (symbol.isNotEmpty() && name.isNotEmpty()) {
                    result.add(StockMeta(symbol = symbol, name = name, market = MarketType.TAIWAN, lastUpdated = timestamp))
                }
            }

            // 下載上櫃股票（TPEX）
            try {
                val otcStocks = ApiClient.tpexService.getOtcStocks()
                otcStocks.forEach { item ->
                    val symbol = item.symbol.trim()
                    val name = item.name.trim()
                    if (symbol.isNotEmpty() && name.isNotEmpty()) {
                        result.add(StockMeta(symbol = symbol, name = name, market = MarketType.TAIWAN, lastUpdated = timestamp))
                    }
                }
            } catch (e: Exception) {
                // TPEX 取得失敗不影響上市資料
            }

            if (result.isNotEmpty()) {
                stockMetaDao.insertStocks(result)
            }
        } catch (e: Exception) {
            // 網路失敗，保留舊有快取，下次再試
        }
    }

    /**
     * 以 symbol 或 name 關鍵字搜尋台股（LIKE 查詢，最多 20 筆）
     */
    suspend fun searchStocks(query: String): List<StockMeta> {
        if (query.isBlank()) return emptyList()
        return stockMetaDao.searchStocks(query.trim())
    }
}
