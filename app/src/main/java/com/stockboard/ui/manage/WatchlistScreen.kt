package com.stockboard.ui.manage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockboard.data.db.WatchlistItem
import com.stockboard.ui.theme.BackgroundDark
import com.stockboard.ui.theme.CardDark
import com.stockboard.ui.theme.ColorDown
import com.stockboard.ui.theme.TextPrimary
import com.stockboard.ui.theme.TextSecondary
import com.stockboard.viewmodel.WatchlistViewModel

/**
 * Task 3-4：自選股刪除頁面
 * - 顯示目前所有自選股列表（Room Flow 訂閱，即時更新）
 * - SwipeToDismiss 向左滑動觸發刪除意圖
 * - AlertDialog 要求確認「確定移除 {name}？」
 * - 確認後呼叫 WatchlistDao.delete()，首頁 StateFlow 即時反映
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(viewModel: WatchlistViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 刪除成功 Snackbar
    LaunchedEffect(uiState.deletedMessage) {
        uiState.deletedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeletedMessage()
        }
    }

    // AlertDialog 確認刪除
    uiState.pendingDeleteItem?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            containerColor = CardDark,
            title = {
                Text("確定移除？", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "確定要從自選股移除「${target.name}（${target.symbol}）」嗎？",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("移除", color = ColorDown)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "自選管理",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        if (uiState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("尚未加入任何自選股\n請至「新增自選股」頁面搜尋", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.items,
                    key = { it.symbol }
                ) { item ->
                    SwipeToDeleteItem(
                        item = item,
                        onSwipeToDelete = { viewModel.requestDelete(item) }
                    )
                }
            }
        }
    }
}

/**
 * 自選股列表項目，套用 SwipeToDismiss
 * 向左滑動 → 顯示紅色背景 + 垃圾桶 → 釋放後觸發 onSwipeToDelete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    item: WatchlistItem,
    onSwipeToDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onSwipeToDelete()
                // 回傳 false 以防止 item 自動消失（待 AlertDialog 確認後才由 Room 刪除）
                false
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // 滑動時的紅色背景提示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorDown.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "刪除",
                    tint = Color.White
                )
            }
        }
    ) {
        // 主要卡片內容
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.symbol,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = item.name,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            // 市場 Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = TextSecondary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = item.market.name,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
