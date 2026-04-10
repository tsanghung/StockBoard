package com.stockboard.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** POST body to mis.taifex.com.tw/fusion/Function/getQuote */
@JsonClass(generateAdapter = true)
data class TaifexQuoteRequest(
    val MarketType: String,              // "0"=日盤 "1"=夜盤
    val SymbolID: List<String> = listOf("TXF-F")
)

@JsonClass(generateAdapter = true)
data class TaifexQuoteResponse(
    @Json(name = "RTCode") val rtCode: String? = null,
    @Json(name = "RTDataList") val rtDataList: List<TaifexQuoteItem>? = null
)

@JsonClass(generateAdapter = true)
data class TaifexQuoteItem(
    @Json(name = "SymbolID") val symbolId: String? = null,
    // 最近成交價（休市時可能為 "-" 或空白）
    @Json(name = "MatchPrice") val matchPrice: String? = null,
    // 今日參考價 / 昨結算價（計算漲跌基準）
    @Json(name = "TodayRefPrice") val todayRefPrice: String? = null,
    // 漲跌點數（帶 "+" / "-" 符號，如 "+100"）
    @Json(name = "PriceChange") val priceChange: String? = null,
    // 漲跌幅百分比（如 "+0.45"，不含 "%" 符號）
    @Json(name = "PriceChangePct") val priceChangePct: String? = null
)
