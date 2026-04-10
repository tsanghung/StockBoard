package com.stockboard.data.network

import com.stockboard.data.model.YahooQuoteResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface YahooFinanceService {
    @GET("v7/finance/quote")
    suspend fun getQuotes(
        @Query("symbols") symbols: String,
        @Query("fields") fields: String = "regularMarketPrice,regularMarketChange,regularMarketChangePercent,regularMarketTime,marketState,shortName",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/91.0.4472.124 Safari/537.36"
    ): YahooQuoteResponse
}
