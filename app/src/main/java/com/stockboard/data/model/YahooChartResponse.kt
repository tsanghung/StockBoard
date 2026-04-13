package com.stockboard.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YahooChartResponse(val chart: YahooChartResult?)

@JsonClass(generateAdapter = true)
data class YahooChartResult(val result: List<YahooChartItem>?)

@JsonClass(generateAdapter = true)
data class YahooChartItem(val meta: YahooChartMeta?)

@JsonClass(generateAdapter = true)
data class YahooChartMeta(
    val symbol: String? = null,
    val shortName: String? = null,
    val regularMarketPrice: Double? = null,
    val previousClose: Double? = null,
    val chartPreviousClose: Double? = null,  // fallback when previousClose 為 null
    val regularMarketTime: Long? = null,
    val exchangeName: String? = null
)
