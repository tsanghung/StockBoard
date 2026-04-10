package com.stockboard.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import com.stockboard.ui.theme.BackgroundDark
import com.stockboard.ui.theme.CardDark
import com.stockboard.ui.theme.ColorUp
import com.stockboard.ui.theme.TextPrimary
import com.stockboard.ui.theme.TextSecondary
import com.stockboard.ui.ads.BannerAdComposable
import com.stockboard.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = "錯誤: $msg")
        }
    }

    // Task 4-1：Rate Limit 提示 Snackbar
    LaunchedEffect(uiState.rateLimitMessage) {
        uiState.rateLimitMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearRateLimitMessage()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("市場總覽", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    // Task 4-1：手動刷新按鈕（受 RateLimiter 60 秒保護）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.isLoading) "更新中..." else "最後更新: ${uiState.lastUpdated}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        IconButton(onClick = { viewModel.manualRefresh() }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "手動刷新",
                                tint = if (uiState.isLoading) TextSecondary else ColorUp,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Task 6-1/6-2: AdMob Banner（50dp）
            BannerAdComposable(
                adUnitId = try {
                    com.stockboard.BuildConfig.ADMOB_BANNER_ID
                } catch (e: Exception) {
                    "ca-app-pub-3940256099942544/6300978111"  // fallback
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 大盤 / 美國
                item { SectionTitle("大盤 / 美國") }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(360.dp).padding(horizontal = 12.dp),
                        userScrollEnabled = false
                    ) {
                        items(uiState.usIndices.size) { i ->
                            val itemUi = uiState.usIndices[i]
                            val name = itemUi.shortName
                            val price = itemUi.price?.let { String.format("%.2f", it) } ?: "0.00"
                            val changeVal = itemUi.change ?: 0.0
                            val pctVal = itemUi.changePercent ?: 0.0
                            val change = String.format("%+.2f", changeVal)
                            val pct = String.format("%+.2f", pctVal)
                            val isUp = when {
                                changeVal > 0 -> true
                                changeVal < 0 -> false
                                else -> null
                            }
                            IndexCard(name, price, change, pct, isUp)
                        }
                    }
                }
                
                // 大盤 / 台灣
                item { SectionTitle("大盤 / 台灣") }
                item {
                    val context = LocalContext.current
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        // 只顯示前兩個指數（上市、上櫃），移除台指期卡片
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height(120.dp),
                            userScrollEnabled = false
                        ) {
                            val twDisplayIndices = uiState.twIndices.take(2)
                            items(twDisplayIndices.size) { i ->
                                val itemUi = twDisplayIndices[i]
                                val name = itemUi.shortName
                                val price = itemUi.price?.let { String.format("%.2f", it) } ?: "0.00"
                                val changeVal = itemUi.change ?: 0.0
                                val pctVal = itemUi.changePercent ?: 0.0
                                val change = String.format("%+.2f", changeVal)
                                val pct = String.format("%+.2f", pctVal)
                                val isUp = when {
                                    changeVal > 0 -> true
                                    changeVal < 0 -> false
                                    else -> null
                                }
                                IndexCard(name, price, change, pct, isUp)
                            }
                        }
                        // 臺股期貨行情超連結卡片（取代原台指期卡片）
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .padding(4.dp)
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.taifex.com.tw/eventTaifexTradingCenter/cht/index.do")
                                    )
                                    context.startActivity(intent)
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .height(80.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "臺股期貨行情",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "點擊前往期交所 →",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // 台股自選
                item { SectionTitle("台股自選") }
                item {
                    val twList = uiState.twWatchlist
                    if (twList.isEmpty()) {
                        androidx.compose.material3.Text(
                            text = "尚未加入台股自選",
                            color = com.stockboard.ui.theme.TextSecondary,
                            fontSize = 13.sp,
                            modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        val rows = (twList.size + 1) / 2
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height((rows * 180).dp).padding(horizontal = 12.dp),
                            userScrollEnabled = false
                        ) {
                            items(twList.size) { i ->
                                val q = twList[i]
                                val price = q.price?.let { String.format("%.2f", it) } ?: "--"
                                val changeVal = q.change ?: 0.0
                                val pctVal = q.changePercent ?: 0.0
                                val change = if (q.price != null) String.format("%+.2f", changeVal) else "--"
                                val pct = if (q.price != null) String.format("%+.2f", pctVal) else "--"
                                val isUp = when {
                                    q.price == null -> null
                                    changeVal > 0 -> true
                                    changeVal < 0 -> false
                                    else -> null
                                }
                                StockCard(q.symbol, q.badgeText, q.name, price, change, pct, isUp)
                            }
                        }
                    }
                }

                // 美股自選
                item { SectionTitle("美股自選") }
                item {
                    val usList = uiState.usWatchlist
                    if (usList.isEmpty()) {
                        androidx.compose.material3.Text(
                            text = "尚未加入美股自選",
                            color = com.stockboard.ui.theme.TextSecondary,
                            fontSize = 13.sp,
                            modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        val rows = (usList.size + 1) / 2
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height((rows * 180).dp).padding(horizontal = 12.dp),
                            userScrollEnabled = false
                        ) {
                            items(usList.size) { i ->
                                val q = usList[i]
                                val price = q.price?.let { String.format("%.2f", it) } ?: "--"
                                val changeVal = q.change ?: 0.0
                                val pctVal = q.changePercent ?: 0.0
                                val change = if (q.price != null) String.format("%+.2f", changeVal) else "--"
                                val pct = if (q.price != null) String.format("%+.2f", pctVal) else "--"
                                val isUp = when {
                                    q.price == null -> null
                                    changeVal > 0 -> true
                                    changeVal < 0 -> false
                                    else -> null
                                }
                                StockCard(q.symbol, q.badgeText, q.name, price, change, pct, isUp)
                            }
                        }
                    }
                }
            }

            // Loading 指示器覆蓋層
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorUp)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
