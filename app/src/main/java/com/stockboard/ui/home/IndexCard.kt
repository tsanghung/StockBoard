package com.stockboard.ui.home

import androidx.compose.foundation.background
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

import androidx.compose.foundation.clickable

@Composable
fun IndexCard(
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
            // 第 1 行：股票代號+Badge (大盤指數無代號行，此處依照 2.3 略過大盤代號)
            
            // 第 2 行：公司 / 指數名稱（細字，灰色）
            Text(text = name, color = TextSecondary, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            
            // 第 3 行：現價（大字，白色）
            Text(text = price, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            // 第 4 行：漲跌額 + 漲跌幅百分比 Badge（漲綠底 / 跌紅底 / 平灰底）
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
