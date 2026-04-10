package com.stockboard.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Finnhub Symbol Search API 回應結構
 * GET https://finnhub.io/api/v1/search?q={keyword}&token={apiKey}
 */
@JsonClass(generateAdapter = true)
data class FinnhubSearchResponse(
    val count: Int = 0,
    val result: List<FinnhubSearchResult> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FinnhubSearchResult(
    val symbol: String,
    val description: String,      // 公司名稱，如 "APPLE INC"
    val type: String,             // 如 "Common Stock", "ETP"
    @Json(name = "displaySymbol") val displaySymbol: String = ""
)
