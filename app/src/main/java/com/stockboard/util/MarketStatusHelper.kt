package com.stockboard.util

/**
 * Task 5-2：市場狀態標註
 *
 * 將 Yahoo Finance marketState 字串轉換為中文狀態顯示
 * 規格書對照：
 * REGULAR       → 交易中
 * CLOSED        → 已收盤
 * POSTPOST      → 已收盤（盤後延伸結束）
 * PRE           → 盤前
 * POST          → 盤後
 * 其他          → 資料更新中
 */
object MarketStatusHelper {
    fun toDisplayStatus(marketState: String?): String {
        return when (marketState?.uppercase()) {
            "REGULAR" -> "🟢 交易中"
            "CLOSED", "POSTPOST" -> "🔴 已收盤"
            "PRE" -> "🟡 盤前"
            "POST" -> "🟠 盤後"
            null -> "⚪ 尚未更新"
            else -> "⚪ 資料更新中"
        }
    }

    /**
     * 依 marketState 判斷是否為活躍交易時段
     * 用於 UI 表示是否需要高亮顯示
     */
    fun isActiveSession(marketState: String?): Boolean {
        return marketState?.uppercase() in setOf("REGULAR", "PRE", "POST")
    }
}
