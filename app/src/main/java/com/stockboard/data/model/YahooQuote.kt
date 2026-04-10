package com.stockboard.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YahooQuoteResponse(
    val quoteResponse: YahooQuoteResult?
)

@JsonClass(generateAdapter = true)
data class YahooQuoteResult(
    val result: List<YahooQuote>?
)

@JsonClass(generateAdapter = true)
data class YahooQuote(
    val symbol: String,
    val shortName: String? = null,
    val regularMarketPrice: Double? = null,
    val regularMarketChange: Double? = null,
    val regularMarketChangePercent: Double? = null,
    val regularMarketTime: Long? = null,
    val marketState: String? = null
)
