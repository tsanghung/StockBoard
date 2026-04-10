package com.stockboard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quote_cache")
data class QuoteCache(
    @PrimaryKey val symbol: String,
    val price: Double,
    val change: Double,
    @ColumnInfo(name = "change_pct") val changePct: Double,
    @ColumnInfo(name = "market_time") val marketTime: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
