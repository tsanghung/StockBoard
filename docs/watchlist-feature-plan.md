# 自選股功能完成規劃

## 現況分析

| 功能 | 狀態 |
|------|------|
| 首頁顯示自選股報價 | ✅ 完成 |
| 新增自選股 UI（ManageScreen） | ✅ 完成並接通 |
| 刪除自選股 UI（WatchlistScreen） | ✅ 完成並接通 |
| 頁面導航（底部導覽列） | ✅ 完成 |
| 美股搜尋改用 Yahoo Finance | ✅ 完成 |
| 防止重複加入 | ✅ 完成 |
| sort_order 排序修正 | ✅ 完成 |

---

## 已實作項目

### 步驟 1 & 2：底部導航列 + 頁面導航

使用 Jetpack Navigation Compose，將 `Scaffold` 提升至最外層 `MainScreen`。

**架構說明：**
- `MainScreen` 為全局 Scaffold，bottomBar 包含 AdMob Banner + NavigationBar
- `NavHost` 套用 `Modifier.padding(innerPadding)` 避免內容被 bottomBar 遮擋
- 三個頁面入口：首頁（Home）、新增自選（Add）、管理清單（Delete）
- `HomeScreen` 的 `bottomBar`（原 AdMob）已移除，改由 `MainScreen` 統一管理

**涉及檔案：**
- `app/build.gradle.kts` — 新增 `androidx.navigation:navigation-compose:2.7.7`
- `ui/nav/AppNavGraph.kt` — 定義 `MainScreen()`，內含 Scaffold + BannerAd + NavigationBar + NavHost
- `MainActivity.kt` — `setContent` 改為 `MainScreen()`
- `ui/home/HomeScreen.kt` — 移除原 `bottomBar`（AdMob Banner）

**底部導覽列三個項目：**

| 項目 | Route | Icon |
|------|-------|------|
| 首頁 | `home` | `Icons.Filled.Home` |
| 新增自選 | `manage` | `Icons.Filled.Add` |
| 管理清單 | `watchlist` | `Icons.Filled.Delete` |

**ViewModel 處理方式：**
- `SearchViewModel` 與 `WatchlistViewModel` 改繼承 `AndroidViewModel`，不需 ViewModelFactory
- `ManageScreen` / `WatchlistScreen` 的 ViewModel 參數加預設值 `= viewModel()`，NavHost 直接呼叫無需傳參

---

### 步驟 3：美股搜尋改用 Yahoo Finance

**問題：** 原 `SearchViewModel` 呼叫 Finnhub API，使用 `demo_token`，無法真正搜尋。

**解法：** 改用 `ApiClient.yahooChartService`（不需 API Key）直接驗證 symbol。Chart API 查無代號會拋出 HTTP 404，以此作為驗證機制。查獲後將結果包裝為 `FinnhubSearchResult`，維持 UI 一致性、減少改動範圍。

**搜尋流程：**
1. 使用者輸入 symbol（如 `AAPL`），自動轉大寫
2. 呼叫 `yahooChartService.getChart(symbol)`
3. 存在且 `regularMarketPrice != null` → 包裝為 `FinnhubSearchResult` 放入 `usResults`
4. 查無 / HTTP 404 / 例外 → `usResults = emptyList()`，`errorMessage` 顯示 Snackbar

**UiState 維持不變：**
- `usResults: List<FinnhubSearchResult>` 保留（通常只有 0 或 1 筆）

**包裝方式：**
```kotlin
FinnhubSearchResult(
    description = meta.symbol ?: symbolParam,
    displaySymbol = meta.symbol ?: symbolParam,
    symbol = meta.symbol ?: symbolParam,
    type = "Common Stock"
)
```
> 若之後需要顯示公司全名，可將 `description` 改為 `meta.shortName`（`YahooChartMeta` 已新增此欄位）。

**涉及檔案：**
- `data/model/YahooChartResponse.kt` — `YahooChartMeta` 新增 `shortName` 欄位（備用）
- `viewmodel/SearchViewModel.kt` — `searchUS()` 改呼叫 `yahooChartService`，結果包裝為 `FinnhubSearchResult`
- `ui/manage/ManageScreen.kt` — UI 無需改動，沿用原 `FinnhubSearchResult` list 顯示邏輯

---

### 步驟 4 & 5：防止重複加入 + sort_order 排序修正

兩個步驟高度相關，統一在新增邏輯中處理。

**防止重複：** 新增前呼叫 `countBySymbol()` 檢查，已存在則透過 `errorMessage` 顯示 Snackbar（與搜尋失敗訊息走同一 channel）。

**排序修正：** `getMaxSortOrder(market)` 改為按市場分區查詢，台股與美股排序各自獨立。fallback 為 `-1`，使第一筆 `sortOrder = 0`，之後依序遞增。

**新增流程（台股 / 美股相同邏輯）：**
1. `countBySymbol(symbol, market)` > 0 → 設 `errorMessage`，return
2. `getMaxSortOrder(market) ?: -1` 取得當前最大值
3. `sortOrder = currentMaxOrder + 1` 插入

**涉及檔案：**
- `data/db/WatchlistDao.kt` — `getMaxSortOrder(market: MarketType)` 加入 `WHERE market = :market` 條件
- `viewmodel/SearchViewModel.kt` — 兩個 add 方法統一套用上述流程；重複時改用 `errorMessage`

---

## 實作完成的所有變更檔案

| 檔案 | 變更內容 |
|------|----------|
| `app/build.gradle.kts` | 新增 navigation-compose 依賴 |
| `ui/nav/AppNavGraph.kt` | 新建 `MainScreen()`（全局導航） |
| `MainActivity.kt` | 改呼叫 `MainScreen()` |
| `ui/home/HomeScreen.kt` | 移除 `bottomBar`（AdMob 移至 MainScreen） |
| `ui/manage/ManageScreen.kt` | ViewModel 預設參數；美股 UI 沿用 FinnhubSearchResult，無需改動 |
| `ui/manage/WatchlistScreen.kt` | ViewModel 預設參數 |
| `data/model/YahooChartResponse.kt` | `YahooChartMeta` 新增 `shortName` |
| `data/db/WatchlistDao.kt` | 新增 `getMaxSortOrder(market: MarketType)`（按市場分區查詢） |
| `viewmodel/SearchViewModel.kt` | 全面改寫：AndroidViewModel、Yahoo Chart 搜尋、重複檢查、排序修正 |
| `viewmodel/WatchlistViewModel.kt` | 改繼承 `AndroidViewModel` |
