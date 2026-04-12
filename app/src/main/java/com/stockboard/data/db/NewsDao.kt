package com.stockboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(news: List<NewsEntity>)

    /**
     * 多維度過濾查詢。
     * - query：標題關鍵字（空字串則略過條件）；呼叫方須先跳脫 /、%、_ 三個特殊字元
     * - sources：來源清單（至少一個，呼叫方必須確保非空）
     * - minPublishTime：最早發布時間（Unix ms）
     *
     * ESCAPE '/' 使用正斜線作為跳脫符號，避免 Kotlin raw string 中反斜線與 SQLite
     * 單引號結合造成解析歧義；/ 本身不是 LIKE 特殊字元，無雙重跳脫問題。
     */
    @Query("""
        SELECT * FROM news_cache
        WHERE (:query = '' OR title LIKE '%' || :query || '%' ESCAPE '/')
        AND source IN (:sources)
        AND publish_time >= :minPublishTime
        ORDER BY publish_time DESC
    """)
    fun getFilteredNews(
        query: String,
        sources: List<String>,
        minPublishTime: Long
    ): Flow<List<NewsEntity>>

    /** 刪除超過 48 小時的舊新聞，由 Worker 呼叫以防止 DB 無限增長 */
    @Query("DELETE FROM news_cache WHERE publish_time < :cutoffTime")
    suspend fun deleteOldNews(cutoffTime: Long)
}
