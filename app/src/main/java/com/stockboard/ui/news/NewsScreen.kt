package com.stockboard.ui.news

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.stockboard.data.db.NewsEntity
import com.stockboard.ui.theme.CardDark
import com.stockboard.ui.theme.ColorUp
import com.stockboard.ui.theme.TextSecondary
import com.stockboard.viewmodel.ALL_NEWS_SOURCES
import com.stockboard.viewmodel.TIME_RANGE_OPTIONS
import com.stockboard.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NewsScreen(newsViewModel: NewsViewModel = viewModel()) {
    val uiState by newsViewModel.uiState.collectAsStateWithLifecycle()
    val searchKeyword by newsViewModel.searchKeyword.collectAsStateWithLifecycle()
    val selectedSources by newsViewModel.selectedSources.collectAsStateWithLifecycle()
    val timeRangeHours by newsViewModel.timeRangeHours.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // 頂部控制面板
        NewsFilterPanel(
            searchKeyword = searchKeyword,
            selectedSources = selectedSources,
            timeRangeHours = timeRangeHours,
            onKeywordChange = newsViewModel::onSearchKeywordChange,
            onSourceToggle = newsViewModel::onSourceToggle,
            onTimeRangeChange = newsViewModel::onTimeRangeSliderChange
        )

        // 新聞列表
        if (uiState.newsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "目前無新聞，請等待背景同步",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.newsList,
                    key = { it.id }
                ) { news ->
                    NewsCard(news = news)
                }
            }
        }
    }
}

@Composable
private fun NewsFilterPanel(
    searchKeyword: String,
    selectedSources: Set<String>,
    timeRangeHours: Int,
    onKeywordChange: (String) -> Unit,
    onSourceToggle: (String) -> Unit,
    onTimeRangeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 關鍵字搜尋框
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = onKeywordChange,
            label = { Text("搜尋新聞標題") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 來源過濾 FilterChip（橫向捲動）
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ALL_NEWS_SOURCES) { source ->
                FilterChip(
                    selected = source in selectedSources,
                    onClick = { onSourceToggle(source) },
                    label = { Text(source) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorUp.copy(alpha = 0.2f),
                        selectedLabelColor = ColorUp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 時間範圍拉桿
        val sliderIndex = TIME_RANGE_OPTIONS.indexOf(timeRangeHours).coerceAtLeast(0).toFloat()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "時間範圍：",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = sliderIndex,
                onValueChange = { onTimeRangeChange(it.roundToInt()) },
                valueRange = 0f..2f,
                steps = 1,   // 0、1、2 三個刻度
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${timeRangeHours} 小時",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(52.dp)
            )
        }
    }
}

@Composable
private fun NewsCard(news: NewsEntity) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                CustomTabsIntent
                    .Builder()
                    .build()
                    .launchUrl(context, Uri.parse(news.url))
            },
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 新聞封面圖（Coil 非同步載入）
            if (!news.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(news.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "新聞圖片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 標題 + 來源 + 時間
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = news.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = news.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorUp
                    )
                    Text(
                        text = "  ·  ${formatPublishTime(news.publishTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private fun formatPublishTime(timestampMs: Long): String {
    // maxOf 防護：手機與伺服器時鐘存在微小誤差時，elapsed 可能為負，
    // 不加保護會渲染出「-2 分鐘前」等異常文字
    val elapsed = maxOf(0L, System.currentTimeMillis() - timestampMs)
    return when {
        elapsed < 3600_000L -> "${elapsed / 60_000} 分鐘前"
        elapsed < 86400_000L -> "${elapsed / 3600_000} 小時前"
        else -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestampMs))
    }
}
