package com.stockboard.util

import java.time.Instant
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Task 5-1：數據日期驗證（多時區）v1.1
 *
 * 依各市場本地時區驗證 regularMarketTime，分類為：
 * - TODAY          → 今日有效數據（正常顯示）
 * - PREVIOUS_CLOSE → 昨收（盤前時段、週末、或市場休市合理狀態）
 * - STALE          → 異常：超過多個交易日未更新
 *
 * v1.1 關鍵修正：美股用 America/New_York 時區判斷，
 * 避免台灣凌晨 0:00~9:30 之間誤判美股收盤數據為 STALE
 */
sealed class DataStatus {
    object TODAY : DataStatus()
    object PREVIOUS_CLOSE : DataStatus()
    object STALE : DataStatus()
}

object DateValidator {

    private val TZ_TAIWAN = ZoneId.of("Asia/Taipei")
    private val TZ_US = ZoneId.of("America/New_York")
    private val TZ_JAPAN = ZoneId.of("Asia/Tokyo")

    /**
     * 各市場盤前開始時間（HHMM 格式）
     * 規格書 v1.1：台股 08:45 前 / 美股 09:30 ET 前，昨日數據標示 PREVIOUS_CLOSE，非 STALE
     */
    private val PRE_MARKET_HHMM = mapOf(
        MarketType.TAIWAN to 845,   // 08:45
        MarketType.US to 930,       // 09:30 ET
        MarketType.JAPAN to 900    // 09:00 JST
    )

    /**
     * 驗證報價時間，回傳該數據的有效性狀態
     *
     * @param regularMarketTime  Yahoo Finance 回傳的 regularMarketTime（Unix 秒）
     * @param market             MarketType（TAIWAN / US / JAPAN）
     */
    fun validate(regularMarketTime: Long, market: MarketType): DataStatus {
        if (regularMarketTime <= 0L) return DataStatus.STALE

        val tz = when (market) {
            MarketType.TAIWAN -> TZ_TAIWAN
            MarketType.US -> TZ_US
            MarketType.JAPAN -> TZ_JAPAN
        }

        val dataDate = Instant.ofEpochSecond(regularMarketTime).atZone(tz).toLocalDate()
        val nowInMkt = ZonedDateTime.now(tz)
        val todayInMkt = nowInMkt.toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(dataDate, todayInMkt)

        return when {
            // 今日數據 → 正常
            daysDiff == 0L -> DataStatus.TODAY

            // 超過 5 天 → STALE 一定有問題
            daysDiff > 5L -> DataStatus.STALE

            // 距昨日一天：判斷當地時間是否仍在盤前
            daysDiff == 1L && isBeforeMarketOpen(market, nowInMkt) -> DataStatus.PREVIOUS_CLOSE

            // 週末（週六/日）持有收盤數據為正常
            isWeekend(todayInMkt) -> DataStatus.PREVIOUS_CLOSE

            // 昨收但已過開盤時間 → 也標 PREVIOUS_CLOSE（等第一筆今日報價進來）
            daysDiff == 1L -> DataStatus.PREVIOUS_CLOSE

            // 超過 1 天且非週末且過開盤 → STALE
            else -> DataStatus.STALE
        }
    }

    /**
     * 將 DataStatus 轉換為 IndexCard 底部顯示的標注文字
     * TODAY 回傳 null（不顯示標注）
     */
    fun toLabel(status: DataStatus): String? = when (status) {
        DataStatus.TODAY -> null
        DataStatus.PREVIOUS_CLOSE -> "昨收"
        DataStatus.STALE -> "⚠ 數據異常"
    }

    private fun isBeforeMarketOpen(market: MarketType, nowInMkt: ZonedDateTime): Boolean {
        val timeInt = nowInMkt.hour * 100 + nowInMkt.minute
        return timeInt < (PRE_MARKET_HHMM[market] ?: 900)
    }

    private fun isWeekend(date: java.time.LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }
}
