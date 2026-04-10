package com.stockboard.data.model

import com.squareup.moshi.JsonClass

/**
 * TWSE MIS 市場資訊觀測站即時報價
 * GET https://mis.twse.com.tw/stock/api/getStockInfo.jsp?ex_ch=tse_t00.tw|otc_o00.tw
 * z = 最近成交價（市場關閉時可能為 "-"）
 * y = 昨收價
 */
@JsonClass(generateAdapter = true)
data class TwseMisResponse(
    val msgArray: List<TwseMisItem>? = null
)

@JsonClass(generateAdapter = true)
data class TwseMisItem(
    val ex: String? = null,   // "tse"（上市）或 "otc"（上櫃）
    val ch: String? = null,   // "t00.tw" 或 "o00.tw"
    val z: String? = null,    // 最近成交價（"-" 表示休市）
    val y: String? = null     // 昨收價
)
