package com.stockboard.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TwelveDataQuote(
    val symbol: String? = null,
    val name: String? = null,
    val close: String? = null,
    val change: String? = null,
    @Json(name = "percent_change") val percentChange: String? = null,
    @Json(name = "is_market_open") val isMarketOpen: Boolean? = null
)
