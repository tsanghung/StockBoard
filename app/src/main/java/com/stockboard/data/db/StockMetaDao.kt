package com.stockboard.data.db

import androidx.room.*

@Dao
interface StockMetaDao {
    @Query("SELECT * FROM stock_meta WHERE symbol LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' LIMIT 20")
    suspend fun searchStocks(query: String): List<StockMeta>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockMeta>)

    @Query("SELECT COUNT(*) FROM stock_meta")
    suspend fun getCount(): Int

    /** 24 小時快取策略：取最新的 last_updated 時間戳 */
    @Query("SELECT MAX(last_updated) FROM stock_meta")
    suspend fun getLastUpdated(): Long?
}

