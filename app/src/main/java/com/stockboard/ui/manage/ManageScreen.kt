package com.stockboard.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockboard.data.db.StockMeta
import com.stockboard.data.model.FinnhubSearchResult
import com.stockboard.ui.theme.BackgroundDark
import com.stockboard.ui.theme.CardDark
import com.stockboard.ui.theme.ColorUp
import com.stockboard.ui.theme.TextPrimary
import com.stockboard.ui.theme.TextSecondary
import com.stockboard.viewmodel.SearchMode
import com.stockboard.viewmodel.SearchViewModel

/**
 * Task 3-2 / 3-3 合體：台股 + 美股搜尋與新增頁面
 * - Tab Row 切換台股 / 美股模式
 * - 台股：Room LIKE 查詢（24h 快取 TWSE OpenAPI）
 * - 美股：Finnhub Search API，過濾 Common Stock / ETP
 */
@Composable
fun ManageScreen(viewModel: SearchViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.addedMessage) {
        uiState.addedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAddedMessage()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "新增自選股",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                // Tab Row：台股 / 美股切換
                TabRow(
                    selectedTabIndex = if (uiState.mode == SearchMode.TAIWAN) 0 else 1,
                    containerColor = BackgroundDark,
                    contentColor = ColorUp
                ) {
                    Tab(
                        selected = uiState.mode == SearchMode.TAIWAN,
                        onClick = { viewModel.switchMode(SearchMode.TAIWAN) },
                        text = {
                            Text(
                                "台股",
                                color = if (uiState.mode == SearchMode.TAIWAN) ColorUp else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = uiState.mode == SearchMode.US,
                        onClick = { viewModel.switchMode(SearchMode.US) },
                        text = {
                            Text(
                                "美股",
                                color = if (uiState.mode == SearchMode.US) ColorUp else TextSecondary
                            )
                        }
                    )
                }

                // 搜尋框
                val hint = if (uiState.mode == SearchMode.TAIWAN)
                    "輸入股號或股名，如 2330 或 台積"
                else
                    "輸入美股代號，如 AAPL"

                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    placeholder = { Text(hint, color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "搜尋", tint = TextSecondary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ColorUp,
                        unfocusedBorderColor = TextSecondary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ColorUp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val msg = if (uiState.mode == SearchMode.TAIWAN)
                            "正在下載台股對照表..." else "搜尋美股中..."
                        Text(msg, color = TextSecondary, fontSize = 13.sp)
                    }
                }

                uiState.mode == SearchMode.TAIWAN -> {
                    if (uiState.query.isNotBlank() && uiState.twResults.isEmpty()) {
                        Text(
                            "查無台股結果「${uiState.query}」",
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.twResults) { stock ->
                                TwStockRow(stock = stock, onAdd = { viewModel.addTaiwanStockToWatchlist(stock) })
                            }
                        }
                    }
                }

                uiState.mode == SearchMode.US -> {
                    if (uiState.query.isNotBlank() && uiState.usResults.isEmpty()) {
                        Text(
                            "查無美股結果「${uiState.query}」",
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.usResults) { stock ->
                                UsStockRow(stock = stock, onAdd = { viewModel.addUsStockToWatchlist(stock) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TwStockRow(stock: StockMeta, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = stock.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = stock.name, color = TextSecondary, fontSize = 13.sp)
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = "新增台股", tint = ColorUp)
        }
    }
}

@Composable
private fun UsStockRow(stock: FinnhubSearchResult, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = stock.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                // 類型徽章（US / ETF）
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ColorUp.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (stock.type == "ETP") "ETF" else "US",
                        color = ColorUp,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(text = stock.description, color = TextSecondary, fontSize = 13.sp)
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = "新增美股", tint = ColorUp)
        }
    }
}
