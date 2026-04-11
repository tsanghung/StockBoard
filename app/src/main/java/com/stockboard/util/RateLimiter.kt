package com.stockboard.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RateLimiter {
    var lastRefreshTime = 0L

    // 最小刷新間隔 60 秒
    const val MINIMUM_INTERVAL_MS = 60_000L

    @Synchronized
    fun canRefresh(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime >= MINIMUM_INTERVAL_MS) {
            lastRefreshTime = now
            return true
        }
        return false
    }

    @Synchronized
    fun getLastRefreshTimeFormatted(): String {
        if (lastRefreshTime == 0L) return "尚未更新"
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(Date(lastRefreshTime))
    }
}
