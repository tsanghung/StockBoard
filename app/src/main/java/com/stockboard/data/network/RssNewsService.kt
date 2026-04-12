package com.stockboard.data.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * 以 @Url 動態指定完整 RSS URL，不依賴固定 baseUrl。
 * Retrofit 原生支援回傳 ResponseBody，無需額外 Converter。
 */
interface RssNewsService {
    @GET
    suspend fun fetchRss(@Url url: String): ResponseBody
}
