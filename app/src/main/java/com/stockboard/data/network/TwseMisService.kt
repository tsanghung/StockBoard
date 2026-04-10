package com.stockboard.data.network

import com.stockboard.data.model.TwseMisResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TwseMisService {
    /**
     * 取得上市（tse_t00.tw）與上櫃（otc_o00.tw）即時指數
     * 需要 Referer 模擬瀏覽器行為，否則被阻擋
     */
    @GET("stock/api/getStockInfo.jsp")
    suspend fun getIndexInfo(
        @Query("ex_ch") symbols: String = "tse_t00.tw|otc_o00.tw",
        @Header("Referer") referer: String = "https://mis.twse.com.tw/stock/fibest.html",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
        @Header("Accept") accept: String = "application/json, text/javascript, */*; q=0.01",
        @Header("X-Requested-With") xRequestedWith: String = "XMLHttpRequest"
    ): TwseMisResponse
}
