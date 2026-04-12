package com.stockboard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_cache")
data class NewsEntity(
    /** URL 的 MD5 雜湊值，確保同一則新聞不重複寫入 */
    @PrimaryKey
    val id: String,

    val title: String,

    /** 來源識別字："Yahoo" | "CNA" | "TechNews" | "Inside" */
    val source: String,

    /** Unix Timestamp（毫秒） */
    @ColumnInfo(name = "publish_time")
    val publishTime: Long,

    val url: String,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?
)
