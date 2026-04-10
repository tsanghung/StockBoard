package com.stockboard.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RateLimiter {
    private var lastRefreshTimeMs = 0L

    // 最小刷新間隔 60 秒
    const val MINIMUM_INTERVAL_MS = 60_000L

    @Synchronized
    fun canRefresh(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTimeMs >= MINIMUM_INTERVAL_MS) {
            lastRefreshTimeMs = now
            return true
        }
        return false
    }

    @Synchronized
    fun getLastRefreshTimeFormatted(): String {
        if (lastRefreshTimeMs == 0L) return "尚未更新"
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(Date(lastRefreshTimeMs))
    }
}
