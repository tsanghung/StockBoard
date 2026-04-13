# 點擊大盤指數卡片導向外部網頁 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 點擊「道瓊工業、NASDAQ、S&P 500、費半 SOX、台積電 ADR」五張大盤指數卡片時，自動開啟外部瀏覽器並跳轉至對應的 Google Finance 頁面（YTD 走勢）。

**Architecture:** 採用資料驅動架構（Data-Driven）。目標網址統一存放在 `HomeViewModel` 的 `usMarketSymbols`（由 `Pair` 改為 `Triple`），並透過 `UsIndexUiModel.url` 傳遞至 UI 層。`IndexCard` 元件僅負責渲染與轉發點擊事件，由 `HomeScreen` 決定是否呼叫系統瀏覽器。

**Tech Stack:** Kotlin、Jetpack Compose、Material3 Card、Android Intent (ACTION_VIEW)

---

## 檔案異動總覽

| 動作 | 路徑 | 說明 |
|------|------|------|
| Modify | `app/src/main/java/com/stockboard/viewmodel/HomeViewModel.kt` | 加入 `url` 欄位至 `UsIndexUiModel`；`usMarketSymbols` 改 Triple；兩處建立 model 需補傳 url |
| Modify | `app/src/main/java/com/stockboard/ui/home/IndexCard.kt` | 加入 `onClick` 參數；Card 加 `.clickable`；補 import |
| Modify | `app/src/main/java/com/stockboard/ui/home/HomeScreen.kt` | 美國指數 item 區塊加 `context`；`IndexCard` 呼叫點傳入 `onClick` |

---

## Task 1：擴充 `UsIndexUiModel` 資料結構

**Files:**
- Modify: `app/src/main/java/com/stockboard/viewmodel/HomeViewModel.kt:24-30`

- [ ] **Step 1：在 `UsIndexUiModel` 加入 `url` 欄位**

  找到第 24–30 行的 `UsIndexUiModel`，將：

  ```kotlin
  data class UsIndexUiModel(
      val symbol: String,
      val shortName: String,
      val price: Double? = null,
      val change: Double? = null,
      val changePercent: Double? = null
  )
  ```

  改為：

  ```kotlin
  data class UsIndexUiModel(
      val symbol: String,
      val shortName: String,
      val price: Double? = null,
      val change: Double? = null,
      val changePercent: Double? = null,
      val url: String? = null
  )
  ```

- [ ] **Step 2：將 `usMarketSymbols` 由 `Pair` 改為 `Triple`，加入對應網址**

  找到第 62–68 行的 `usMarketSymbols`，將：

  ```kotlin
  private val usMarketSymbols = listOf(
      "^DJI"  to "道瓊工業",
      "^IXIC" to "NASDAQ",
      "^GSPC" to "S&P 500",
      "^SOX"  to "費半 SOX",
      "TSM"   to "台積電 ADR"
  )
  ```

  改為：

  ```kotlin
  private val usMarketSymbols = listOf(
      Triple("^DJI",  "道瓊工業",   "https://www.google.com/finance/beta/quote/.DJI:INDEXDJX?hl=zh-TW&window=YTD"),
      Triple("^IXIC", "NASDAQ",     "https://www.google.com/finance/beta/quote/.IXIC:INDEXNASDAQ?hl=zh-TW&window=YTD"),
      Triple("^GSPC", "S&P 500",   "https://www.google.com/finance/beta/quote/.INX:INDEXSP?hl=zh-TW&window=YTD"),
      Triple("^SOX",  "費半 SOX",   "https://www.google.com/finance/beta/quote/SOX:INDEXNASDAQ?hl=zh-TW&window=YTD"),
      Triple("TSM",   "台積電 ADR", "https://www.google.com/finance/beta/quote/TSM:NYSE?hl=zh-TW&window=YTD")
  )
  ```

- [ ] **Step 3：更新 `init` 區塊的佔位 model，傳入 url**

  找到第 76–78 行的 `init` 區塊，將：

  ```kotlin
  usIndices = usMarketSymbols.map { UsIndexUiModel(it.first, it.second) }
  ```

  改為：

  ```kotlin
  usIndices = usMarketSymbols.map { UsIndexUiModel(it.first, it.second, url = it.third) }
  ```

- [ ] **Step 4：更新 `fetchYahooUsIndices()` 建立 model 時傳入 url**

  找到第 162–179 行的 `fetchYahooUsIndices()`，解構語法由：

  ```kotlin
  val results = usMarketSymbols.map { (symbol, name) ->
      async {
          try {
              ...
              UsIndexUiModel(symbol, name, price, change, pct)
          } catch (e: Exception) {
              UsIndexUiModel(symbol, name)
          }
      }
  }.awaitAll()
  ```

  改為：

  ```kotlin
  val results = usMarketSymbols.map { (symbol, name, url) ->
      async {
          try {
              ...
              UsIndexUiModel(symbol, name, price, change, pct, url)
          } catch (e: Exception) {
              UsIndexUiModel(symbol, name, url = url)
          }
      }
  }.awaitAll()
  ```

- [ ] **Step 5：確認編譯無誤（建置驗證）**

  在 Android Studio 執行 **Build > Make Project**（或 `Ctrl+F9`），確認無編譯錯誤。

- [ ] **Step 6：Commit**

  ```bash
  git add app/src/main/java/com/stockboard/viewmodel/HomeViewModel.kt
  git commit -m "feat: add url field to UsIndexUiModel and bind Google Finance URLs"
  ```

---

## Task 2：`IndexCard` 加入 `onClick` 事件支援

**Files:**
- Modify: `app/src/main/java/com/stockboard/ui/home/IndexCard.kt`

- [ ] **Step 1：加入 `clickable` import**

  在 `IndexCard.kt` 頂部 import 區塊，補上：

  ```kotlin
  import androidx.compose.foundation.clickable
  ```

- [ ] **Step 2：在函式簽名加入 `onClick` 參數**

  找到第 23–29 行的 `fun IndexCard(...)`，將：

  ```kotlin
  @Composable
  fun IndexCard(
      name: String,
      price: String,
      change: String,
      changePct: String,
      isUp: Boolean?
  ) {
  ```

  改為：

  ```kotlin
  @Composable
  fun IndexCard(
      name: String,
      price: String,
      change: String,
      changePct: String,
      isUp: Boolean?,
      onClick: () -> Unit = {}
  ) {
  ```

- [ ] **Step 3：在 `Card` 的 `Modifier` 加上 `.clickable`**

  找到第 36–40 行的 `Card(...)`，將：

  ```kotlin
  Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = CardDark),
      modifier = Modifier.fillMaxWidth().padding(4.dp)
  ) {
  ```

  改為：

  ```kotlin
  Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = CardDark),
      modifier = Modifier.fillMaxWidth().padding(4.dp).clickable { onClick() }
  ) {
  ```

- [ ] **Step 4：確認編譯無誤**

  在 Android Studio 執行 **Build > Make Project**，確認無編譯錯誤。

- [ ] **Step 5：Commit**

  ```bash
  git add app/src/main/java/com/stockboard/ui/home/IndexCard.kt
  git commit -m "feat: add onClick parameter to IndexCard with clickable modifier"
  ```

---

## Task 3：`HomeScreen` 美股指數區塊接入點擊導向

**Files:**
- Modify: `app/src/main/java/com/stockboard/ui/home/HomeScreen.kt:102-125`

- [ ] **Step 1：在「大盤 / 美國」item 區塊加入 `context`**

  找到第 102–125 行的 `item { /* 大盤 / 美國 */ }` 區塊，在 `LazyVerticalGrid(...)` 之前加入：

  ```kotlin
  item {
      val context = LocalContext.current   // ← 新增這行
      LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          ...
      ) {
  ```

  > 注意：`LocalContext.current` 已在台灣區塊（第 130 行）使用，此處為美股區塊獨立宣告，兩者互不干擾。

- [ ] **Step 2：將 `IndexCard` 呼叫點加入 `onClick`**

  找到第 122 行的 `IndexCard(name, price, change, pct, isUp)`，改為：

  ```kotlin
  IndexCard(
      name = name,
      price = price,
      change = change,
      changePct = pct,
      isUp = isUp,
      onClick = {
          itemUi.url?.let { url ->
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
              context.startActivity(intent)
          }
      }
  )
  ```

- [ ] **Step 3：確認台灣指數的 `IndexCard` 呼叫點不受影響**

  確認第 152 行台灣區塊的 `IndexCard(name, price, change, pct, isUp)` 維持不變（`onClick` 有預設值 `{}`，無需修改）。

- [ ] **Step 4：Build & 手動測試**

  1. 在 Android Studio 執行 **Build > Make Project**，確認無編譯錯誤。
  2. 在模擬器或實機執行 App。
  3. 點擊「道瓊工業」卡片 → 應跳出系統瀏覽器並開啟 Google Finance 道瓊頁面。
  4. 點擊「NASDAQ」、「S&P 500」、「費半 SOX」、「台積電 ADR」各一次，確認各自跳轉正確。
  5. 點擊「上市指數」、「上櫃指數」卡片 → 應無任何反應（url 為 null）。

- [ ] **Step 5：Commit**

  ```bash
  git add app/src/main/java/com/stockboard/ui/home/HomeScreen.kt
  git commit -m "feat: open Google Finance on US index card click via external browser"
  ```
