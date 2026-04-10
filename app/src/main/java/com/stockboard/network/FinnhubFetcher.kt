package com.stockboard.network

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class FinnhubFetcher {

    /**
     * 實作 3.4 Finnhub 節流規範
     * @param marketIndices 大盤指數 (固定 5 檔)
     * @param watchlistedSymbols 自選股 (最多 20 檔)
     * @param fetchAction 針對單一 Symbol 呼叫 API 取資料的方法
     */
    suspend fun fetchWithThrottling(
        marketIndices: List<String>,
        watchlistedSymbols: List<String>,
        fetchAction: suspend (String) -> Unit
    ) = coroutineScope {
        // [策略 1] 優先級：固定大盤先發出請求
        val indicesJob = async {
            processInBatches(marketIndices, fetchAction)
        }

        // [策略 2] 自選股延後 2 秒開始，確保大盤優先取得並錯開瞬間尖峰
        val watchlistJob = async {
            delay(2000L)
            processInBatches(watchlistedSymbols, fetchAction)
        }

        // 等待兩端流程皆跑完
        indicesJob.await()
        watchlistJob.await()
    }

    /**
     * [策略 3] 批次分組：每批最多 10 個 Symbol，批次間 delay 1100ms
     */
    private suspend fun processInBatches(symbols: List<String>, fetchAction: suspend (String) -> Unit) = coroutineScope {
        val chunks = symbols.chunked(10)
        for ((index, chunk) in chunks.withIndex()) {
            // 同批次的 API Async 並行處理
            val deferreds = chunk.map { symbol ->
                async { fetchAction(symbol) }
            }
            deferreds.awaitAll()

            // 若後面還有批次，強制延遲 1100 毫秒，避免觸發 429 Error
            if (index < chunks.size - 1) {
                delay(1100L)
            }
        }
    }
}
