package com.stockboard.data.network

import com.stockboard.data.model.TwelveDataQuote
import retrofit2.http.GET
import retrofit2.http.Query

interface TwelveDataService {
    /**
     * 批次查詢多個指數/股票報價，回傳原始 ResponseBody 再手動解析
     * 避免 Kotlin 型別擦除導致 Moshi 無法處理 Map<String, T> 泛型
     */
    @GET("quote")
    suspend fun getBatchQuotes(
        @Query("symbol") symbol: String,
        @Query("apikey") apikey: String
    ): okhttp3.ResponseBody
}
