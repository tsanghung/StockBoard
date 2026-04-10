package com.stockboard.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteCacheDao {
    @Query("SELECT * FROM quote_cache")
    fun getAllQuotes(): Flow<List<QuoteCache>>

    @Query("SELECT * FROM quote_cache WHERE symbol = :symbol LIMIT 1")
    suspend fun getBySymbol(symbol: String): QuoteCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteCache)

    /** Task 4-2：批次寫入（WorkManager 使用） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<QuoteCache>)

    @Query("DELETE FROM quote_cache")
    suspend fun clearAll()
}
