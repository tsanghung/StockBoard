package com.stockboard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stockboard.util.MarketType

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val name: String,
    val market: MarketType,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "is_fixed") val isFixed: Boolean
)
