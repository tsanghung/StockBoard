package com.stockboard.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FinnhubQuote(
    @Json(name = "c") val currentPrice: Double? = null,
    @Json(name = "d") val change: Double? = null,
    @Json(name = "dp") val changePercent: Double? = null,
    @Json(name = "t") val timestamp: Long? = null
)
