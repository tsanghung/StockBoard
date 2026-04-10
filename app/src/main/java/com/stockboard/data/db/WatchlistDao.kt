package com.stockboard.data.db

import androidx.room.*
import com.stockboard.util.MarketType
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY sort_order ASC")
    fun getAllWatchlist(): Flow<List<WatchlistItem>>

    @Query("SELECT * FROM watchlist WHERE market = :market ORDER BY sort_order ASC")
    fun getByMarket(market: MarketType): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WatchlistItem)

    @Delete
    suspend fun deleteItem(item: WatchlistItem)

    /** Task 3-4：依 symbol 刪除（不需完整物件） */
    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)

    @Query("SELECT COUNT(*) FROM watchlist WHERE symbol = :symbol AND market = :market")
    suspend fun countBySymbol(symbol: String, market: com.stockboard.util.MarketType): Int
}
