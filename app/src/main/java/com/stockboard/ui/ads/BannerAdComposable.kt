package com.stockboard.ui.ads

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Task 6-1：AdMob Banner 廣告 Composable
 *
 * 以 AndroidView 包裝原生 AdView，符合規格書版面要求：
 * - 高度 50dp（AdSize.BANNER 標準尺寸）
 * - 置於 Scaffold bottomBar 的 NavigationBar 上方
 *
 * Task 6-2：adUnitId 由外部傳入，支援測試 / 正式 ID 切換
 * - 測試 adUnitId：ca-app-pub-3940256099942544/6300978111
 * - 正式版從 BuildConfig.ADMOB_BANNER_ID 讀取（local.properties 注入）
 */
@Composable
fun BannerAdComposable(
    adUnitId: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
) {
    AndroidView(
        modifier = modifier,
        factory = { context: Context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // 若 adUnitId 變更時（如 debug → release 切換），重新載入廣告
            if (adView.adUnitId != adUnitId) {
                adView.adUnitId = adUnitId
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    )
}
