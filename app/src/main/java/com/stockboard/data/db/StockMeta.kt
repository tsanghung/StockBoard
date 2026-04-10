package com.stockboard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stockboard.util.MarketType

@Entity(
    tableName = "stock_meta",
    indices = [
        Index(value = ["symbol"]),
        Index(value = ["name"])
    ]
)
data class StockMeta(
    @PrimaryKey val symbol: String,
    val name: String,
    val market: MarketType,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long
)
