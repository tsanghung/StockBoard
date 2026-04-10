package com.stockboard.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * TWSE OpenAPI 回應結構
 * GET https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL
 * 上市：回傳 List<TwseStockItem>
 *
 * 上櫃 (OTC) 另用 https://www.tpex.org.tw/openapi/v1/tpex_mainboard_daily_close_quotes
 * 回傳結構略有不同，故 TpexStockItem 獨立定義
 */
@JsonClass(generateAdapter = true)
data class TwseStockItem(
    @Json(name = "Code") val symbol: String,
    @Json(name = "Name") val name: String
)

@JsonClass(generateAdapter = true)
data class TpexStockItem(
    @Json(name = "SecuritiesCompanyCode") val symbol: String,
    @Json(name = "CompanyName") val name: String
)
