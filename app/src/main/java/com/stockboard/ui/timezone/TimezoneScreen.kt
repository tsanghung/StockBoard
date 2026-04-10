package com.stockboard.ui.timezone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockboard.ui.theme.BackgroundDark
import com.stockboard.ui.theme.CardDark
import com.stockboard.ui.theme.ColorUp
import com.stockboard.ui.theme.TextPrimary
import com.stockboard.ui.theme.TextSecondary
import com.stockboard.util.MarketStatusHelper
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Task 5-2：時區頁面
 * 顯示三個市場（台股 / 美股 / 日股）的：
 * - 市場名稱與旗幟
 * - 當地即時時間
 * - 開盤 / 收盤時間區間
 * - 目前市場狀態（交易中 / 盤前 / 盤後 / 已收盤）
 *
 * 時間每分鐘由 Composable 自動更新（LaunchedEffect + delay）
 */
@Composable
fun TimezoneScreen() {
    // 每分鐘刷新一次時間顯示
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            tick++
        }
    }

    val markets = listOf(
        MarketInfo(
            flag = "🇹🇼",
            name = "台灣股市",
            zoneId = ZoneId.of("Asia/Taipei"),
            openTime = "09:00",
            closeTime = "13:30",
            // marketState 實際上應從 ViewModel StateFlow 傳入，此處以靜態示範
            marketState = null
        ),
        MarketInfo(
            flag = "🇺🇸",
            name = "美國股市",
            zoneId = ZoneId.of("America/New_York"),
            openTime = "09:30",
            closeTime = "16:00",
            marketState = null
        ),
        MarketInfo(
            flag = "🇯🇵",
            name = "日本股市",
            zoneId = ZoneId.of("Asia/Tokyo"),
            openTime = "09:00",
            closeTime = "15:30",
            marketState = null
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Text(
            text = "時區總覽",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(markets) { market ->
                MarketTimezoneCard(market = market, tick = tick)
            }
        }
    }
}

data class MarketInfo(
    val flag: String,
    val name: String,
    val zoneId: ZoneId,
    val openTime: String,
    val closeTime: String,
    val marketState: String?   // 來自 API 的 marketState（如 "REGULAR", "CLOSED"）
)

@Composable
private fun MarketTimezoneCard(market: MarketInfo, tick: Int) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd EEE")
    val now = ZonedDateTime.now(market.zoneId)

    // 根據當地時間自動推算市場狀態（若 API 未提供）
    val displayStatus = when {
        market.marketState != null -> MarketStatusHelper.toDisplayStatus(market.marketState)
        else -> {
            val timeInt = now.hour * 100 + now.minute
            val (openInt, closeInt) = parseHHMM(market.openTime) to parseHHMM(market.closeTime)
            when {
                now.dayOfWeek.value >= 6 -> "🔴 週末休市"           // 週六 = 6, 週日 = 7
                timeInt < openInt - 15 -> "🟡 盤前"
                timeInt in openInt until closeInt -> "🟢 交易中"
                timeInt in closeInt until (closeInt + 100) -> "🟠 盤後"
                else -> "🔴 已收盤"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            // 頂部：旗幟 + 市場名稱 + 狀態
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = market.flag, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = market.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(text = displayStatus, color = TextSecondary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 中部：當地時間（大）
            Text(
                text = now.format(timeFormatter),
                color = ColorUp,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = now.format(dateFormatter),
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 底部：開收盤時間
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("開盤", color = TextSecondary, fontSize = 11.sp)
                    Text(market.openTime, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("收盤", color = TextSecondary, fontSize = 11.sp)
                    Text(market.closeTime, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("時區", color = TextSecondary, fontSize = 11.sp)
                    Text(market.zoneId.id.substringAfter("/"), color = TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}

/** 將 "HH:mm" 字串轉換為整數 (e.g. "09:30" → 930) */
private fun parseHHMM(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 100 + parts[1].toInt()
}
