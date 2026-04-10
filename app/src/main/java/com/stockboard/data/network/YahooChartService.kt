package com.stockboard.data.network

import com.stockboard.data.model.YahooChartResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface YahooChartService {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol", encoded = true) symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1d",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/91.0.4472.124 Safari/537.36"
    ): YahooChartResponse
}
