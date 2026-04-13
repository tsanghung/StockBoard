package com.stockboard.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockboard.ui.theme.CardDark
import com.stockboard.ui.theme.ColorDown
import com.stockboard.ui.theme.ColorFlat
import com.stockboard.ui.theme.ColorUp
import com.stockboard.ui.theme.TextPrimary
import com.stockboard.ui.theme.TextSecondary

@Composable
fun StockCard(
    symbol: String,
    badgeText: String, // "上市", "櫃", "US" 等
    name: String,
    price: String,
    change: String,
    changePct: String,
    isUp: Boolean?,
    onClick: () -> Unit = {}
) {
    val changeColor = when (isUp) {
        true -> ColorUp
        false -> ColorDown
        null -> ColorFlat
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier.fillMaxWidth().padding(4.dp).clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 第 1 行：股票代號 + 公司名稱 + Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = symbol, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = name, color = TextSecondary, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = badgeText,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // 第 2 行：現價
            Text(text = price, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            // 第 3 行：漲跌幅
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(color = changeColor, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$change ($changePct%)",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
