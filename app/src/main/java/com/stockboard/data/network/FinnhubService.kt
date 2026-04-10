package com.stockboard.data.network

import com.stockboard.data.model.FinnhubQuote
import com.stockboard.data.model.FinnhubSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubService {
    @GET("api/v1/quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): FinnhubQuote

    /**
     * Task 3-3：美股關鍵字搜尋
     * 回傳 type = "Common Stock" 或 "ETP" 的標的
     */
    @GET("api/v1/search")
    suspend fun searchSymbol(
        @Query("q") query: String,
        @Query("token") token: String
    ): FinnhubSearchResponse
}
