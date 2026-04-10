package com.stockboard.data.network

import com.stockboard.data.model.TwseStockItem
import com.stockboard.data.model.TpexStockItem
import retrofit2.http.GET

interface TwseService {
    /** 取得所有上市股票清單（每日更新，我們採 24h 快取） */
    @GET("v1/exchangeReport/STOCK_DAY_ALL")
    suspend fun getTseStocks(): List<TwseStockItem>
}

interface TpexService {
    /** 取得所有上櫃股票清單 */
    @GET("v1/tpex_mainboard_daily_close_quotes")
    suspend fun getOtcStocks(): List<TpexStockItem>
}
