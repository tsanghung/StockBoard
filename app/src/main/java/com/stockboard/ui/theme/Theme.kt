package com.stockboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = BackgroundDark,
    surface = CardDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun StockBoardTheme(content: @Composable () -> Unit) {
    // 根據 2.1 設計原則：主題深色模式固定，不跟隨系統
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
