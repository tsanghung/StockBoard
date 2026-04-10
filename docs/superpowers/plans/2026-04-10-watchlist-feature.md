# 自選股功能完整實作計劃

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成自選股功能，包含底部導覽列、Yahoo Finance 搜尋、防止重複加入、及排序修正。

**Architecture:** 使用 Jetpack Navigation Compose 管理頁面，`MainScreen` 作為全局 Scaffold，ViewModel 繼承 `AndroidViewModel`，透過 Yahoo Chart API（無需 Key）驗證股票代號。

**Tech Stack:** Kotlin、Jetpack Compose、Navigation Compose 2.7.7、Room、Yahoo Finance Chart API（透過 `ApiClient.yahooChartService`）

---

## 檔案結構

| 檔案 | 角色 |
|------|------|
| `app/build.gradle.kts` | 新增 navigation-compose 依賴 |
| `app/src/main/java/com/stockboard/ui/nav/AppNavGraph.kt` | 定義全局 `MainScreen()`、NavHost、BottomBar |
| `app/src/main/java/com/stockboard/MainActivity.kt` | 入口改為 `MainScreen()` |
| `app/src/main/java/com/stockboard/ui/home/HomeScreen.kt` | 移除原 bottomBar（AdMob 統一由 MainScreen 管理） |
| `app/src/main/java/com/stockboard/ui/manage/ManageScreen.kt` | ViewModel 預設參數（`= viewModel()`） |
| `app/src/main/java/com/stockboard/ui/manage/WatchlistScreen.kt` | ViewModel 預設參數（`= viewModel()`） |
| `app/src/main/java/com/stockboard/data/model/YahooChartResponse.kt` | `YahooChartMeta` 新增 `shortName` 欄位 |
| `app/src/main/java/com/stockboard/data/db/WatchlistDao.kt` | `getMaxSortOrder(market)` 改為按市場分區查詢 |
| `app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt` | 改為 AndroidViewModel、Yahoo 搜尋、重複檢查、排序修正 |
| `app/src/main/java/com/stockboard/viewmodel/WatchlistViewModel.kt` | 改繼承 `AndroidViewModel` |

---

## Task 1：新增 navigation-compose 依賴

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1：確認 build.gradle.kts 已包含 navigation-compose**

```bash
grep -n "navigation-compose" app/build.gradle.kts
```
Expected: 找到 `androidx.navigation:navigation-compose:2.7.7`

- [ ] **Step 2：若未包含，加入依賴**

在 `dependencies { }` 區塊加入：
```kotlin
implementation("androidx.navigation:navigation-compose:2.7.7")
```

- [ ] **Step 3：同步 Gradle**

```bash
./gradlew dependencies --configuration releaseRuntimeClasspath | grep navigation
```
Expected: 看到 `navigation-compose` 出現在依賴樹

- [ ] **Step 4：Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: add navigation-compose dependency"
```

---

## Task 2：建立全局導覽架構（MainScreen + AppNavGraph）

**Files:**
- Create/Modify: `app/src/main/java/com/stockboard/ui/nav/AppNavGraph.kt`
- Modify: `app/src/main/java/com/stockboard/MainActivity.kt`

- [ ] **Step 1：確認 AppNavGraph.kt 定義了 `MainScreen()`**

```bash
grep -n "fun MainScreen" app/src/main/java/com/stockboard/ui/nav/AppNavGraph.kt
```
Expected: 找到 `@Composable fun MainScreen()`

- [ ] **Step 2：確認 MainScreen 包含三個導覽項目**

`AppNavGraph.kt` 中應有：
```kotlin
val items = listOf(
    BottomNavItem("home", Icons.Filled.Home, "首頁"),
    BottomNavItem("manage", Icons.Filled.Add, "新增自選"),
    BottomNavItem("watchlist", Icons.Filled.Delete, "管理清單")
)
```

- [ ] **Step 3：確認 MainActivity 改為呼叫 MainScreen**

```bash
grep -n "MainScreen\|setContent" app/src/main/java/com/stockboard/MainActivity.kt
```
Expected：`setContent { MainScreen() }`（或包在 theme 內）

- [ ] **Step 4：確認 HomeScreen 已移除原 bottomBar**

```bash
grep -n "bottomBar\|BannerAd" app/src/main/java/com/stockboard/ui/home/HomeScreen.kt
```
Expected：HomeScreen 內不再有 `bottomBar = { BannerAd(...) }`

- [ ] **Step 5：Build 確認編譯成功**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected：`BUILD SUCCESSFUL`

- [ ] **Step 6：Commit**

```bash
git add app/src/main/java/com/stockboard/ui/nav/AppNavGraph.kt \
        app/src/main/java/com/stockboard/MainActivity.kt \
        app/src/main/java/com/stockboard/ui/home/HomeScreen.kt
git commit -m "feat: add bottom navigation with MainScreen scaffold"
```

---

## Task 3：ManageScreen / WatchlistScreen ViewModel 預設參數

**Files:**
- Modify: `app/src/main/java/com/stockboard/ui/manage/ManageScreen.kt`
- Modify: `app/src/main/java/com/stockboard/ui/manage/WatchlistScreen.kt`

- [ ] **Step 1：確認 ManageScreen 的 ViewModel 有預設值**

```bash
grep -n "fun ManageScreen" app/src/main/java/com/stockboard/ui/manage/ManageScreen.kt
```
Expected：`fun ManageScreen(viewModel: SearchViewModel = viewModel())`

- [ ] **Step 2：確認 WatchlistScreen 的 ViewModel 有預設值**

```bash
grep -n "fun WatchlistScreen" app/src/main/java/com/stockboard/ui/manage/WatchlistScreen.kt
```
Expected：`fun WatchlistScreen(viewModel: WatchlistViewModel = viewModel())`

- [ ] **Step 3：Build 確認**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```
Expected：`BUILD SUCCESSFUL`

- [ ] **Step 4：Commit**

```bash
git add app/src/main/java/com/stockboard/ui/manage/ManageScreen.kt \
        app/src/main/java/com/stockboard/ui/manage/WatchlistScreen.kt
git commit -m "refactor: add default viewModel() params to ManageScreen and WatchlistScreen"
```

---

## Task 4：ViewModel 改繼承 AndroidViewModel

**Files:**
- Modify: `app/src/main/java/com/stockboard/viewmodel/WatchlistViewModel.kt`
- Modify: `app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt`

- [ ] **Step 1：確認 WatchlistViewModel 繼承 AndroidViewModel**

```bash
grep -n "AndroidViewModel\|ViewModel(" app/src/main/java/com/stockboard/viewmodel/WatchlistViewModel.kt
```
Expected：`class WatchlistViewModel(application: Application) : AndroidViewModel(application)`

- [ ] **Step 2：確認 SearchViewModel 繼承 AndroidViewModel**

```bash
grep -n "AndroidViewModel\|ViewModel(" app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt
```
Expected：`class SearchViewModel(application: Application) : AndroidViewModel(application)`

- [ ] **Step 3：Build 確認**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```
Expected：`BUILD SUCCESSFUL`

- [ ] **Step 4：Commit**

```bash
git add app/src/main/java/com/stockboard/viewmodel/WatchlistViewModel.kt \
        app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt
git commit -m "refactor: migrate ViewModels to AndroidViewModel"
```

---

## Task 5：美股搜尋改用 Yahoo Finance Chart API

**Files:**
- Modify: `app/src/main/java/com/stockboard/data/model/YahooChartResponse.kt`
- Modify: `app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt`

- [ ] **Step 1：確認 YahooChartMeta 有 shortName 欄位**

```bash
grep -n "shortName" app/src/main/java/com/stockboard/data/model/YahooChartResponse.kt
```
Expected：`val shortName: String? = null`

- [ ] **Step 2：確認 searchUS() 呼叫 yahooChartService 而非 Finnhub**

```bash
grep -n "yahooChartService\|finnhub\|searchUS" app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt
```
Expected：看到 `yahooChartService.getChart(symbol)`，不再有 `finnhubService.search`

- [ ] **Step 3：確認搜尋結果包裝邏輯**

`SearchViewModel.kt` 中應有：
```kotlin
FinnhubSearchResult(
    description = meta.symbol ?: symbolParam,
    displaySymbol = meta.symbol ?: symbolParam,
    symbol = meta.symbol ?: symbolParam,
    type = "Common Stock"
)
```

- [ ] **Step 4：Build 確認**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```
Expected：`BUILD SUCCESSFUL`

- [ ] **Step 5：Commit**

```bash
git add app/src/main/java/com/stockboard/data/model/YahooChartResponse.kt \
        app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt
git commit -m "feat: replace Finnhub search with Yahoo Finance Chart API validation"
```

---

## Task 6：防止重複加入 + sort_order 按市場分區排序

**Files:**
- Modify: `app/src/main/java/com/stockboard/data/db/WatchlistDao.kt`
- Modify: `app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt`

- [ ] **Step 1：確認 WatchlistDao 有按市場查詢的 getMaxSortOrder**

```bash
grep -n "getMaxSortOrder\|WHERE market" app/src/main/java/com/stockboard/data/db/WatchlistDao.kt
```
Expected：`WHERE market = :market` 出現在 `getMaxSortOrder` 的 SQL 中

- [ ] **Step 2：確認 SearchViewModel 的 add 方法有重複檢查**

```bash
grep -n "countBySymbol\|already\|重複" app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt
```
Expected：看到 `countBySymbol` 呼叫，且 > 0 時設 `errorMessage`

- [ ] **Step 3：確認加入邏輯（以 addUS 為例）**

`SearchViewModel.kt` 中應有：
```kotlin
val count = dao.countBySymbol(symbol, MarketType.US)
if (count > 0) {
    _uiState.update { it.copy(errorMessage = "已在自選股中") }
    return@launch
}
val maxOrder = dao.getMaxSortOrder(MarketType.US) ?: -1
dao.insert(WatchlistItem(symbol = symbol, market = MarketType.US, sortOrder = maxOrder + 1))
```

- [ ] **Step 4：Build 確認**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```
Expected：`BUILD SUCCESSFUL`

- [ ] **Step 5：Commit**

```bash
git add app/src/main/java/com/stockboard/data/db/WatchlistDao.kt \
        app/src/main/java/com/stockboard/viewmodel/SearchViewModel.kt
git commit -m "feat: prevent duplicate watchlist entries and fix per-market sort order"
```

---

## Task 7：整合驗證 — 完整 Build + 手動測試清單

- [ ] **Step 1：執行完整 Debug Build**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```
Expected：`BUILD SUCCESSFUL`

- [ ] **Step 2：手動測試底部導覽**

啟動 App，確認：
- 點「首頁」→ HomeScreen 顯示自選股報價
- 點「新增自選」→ ManageScreen 可搜尋股票
- 點「管理清單」→ WatchlistScreen 顯示已加股票並可刪除
- AdMob Banner 統一顯示在底部導覽列上方

- [ ] **Step 3：手動測試美股搜尋**

在 ManageScreen 輸入 `AAPL`：
- 應顯示搜尋結果（AAPL）
- 點加入 → 首頁出現 AAPL 報價

輸入不存在代號如 `XXXZZZ`：
- 應顯示 Snackbar 錯誤訊息

- [ ] **Step 4：手動測試防重複**

再次搜尋並嘗試加入已在清單的 `AAPL`：
- 應顯示 Snackbar 提示已存在，不重複新增

- [ ] **Step 5：手動測試排序**

加入多支美股（如 AAPL、MSFT、GOOGL）：
- 首頁顯示順序應與加入順序一致

- [ ] **Step 6：最終 Commit（若有剩餘未提交修改）**

```bash
git status
git add -p  # 逐一確認剩餘修改
git commit -m "chore: finalize watchlist feature implementation"
```
