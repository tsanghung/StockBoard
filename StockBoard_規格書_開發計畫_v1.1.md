**StockBoard for Android**

股市看板 APP

完整規格書 & Vibe Coding 開發計畫

版本：v1.1　　日期：2025-04-09　　狀態：定稿

_v1.1 修訂：台股對照表快取策略 / Finnhub 節流機制 / 防呆多時區邏輯 / AdMob 版面規範_

**1\. 專案概述**
============

**1.1 產品定義**
------------

一款 Android 原生股市看板 APP，專為個人投資者設計，整合台股、美股、日股大盤指數與自選個股報價，以簡潔深色介面呈現最新股價數據。

**1.2 商業模式**
------------

*   發布平台：Google Play Store
*   定價策略：免費下載
*   營收來源：Google AdMob 廣告（Banner 廣告）
*   目標用戶：台灣個人投資者，關注台股與美股行情

**1.3 核心功能**
------------

*   首頁一頁式捲動：大盤指數（美國 / 日本 / 台灣）+ 台股自選 + 美股自選
*   台股自選股：新增 / 刪除，支援上市（TSE）與上櫃（OTC）個股
*   美股自選股：新增 / 刪除，支援所有 NYSE / NASDAQ 個股
*   自動定期更新：前台每 5 分鐘、背景 WorkManager 每 15 分鐘刷新
*   數據防呆：依各市場時區驗證數據日期，避免顯示過期或誤判異常
*   廣告整合：AdMob Banner 廣告，符合 AdMob 政策規範

**2\. UI/UX 規格（定稿）**
====================

**2.1 設計原則**
------------

*   主題：深色模式固定，不跟隨系統
*   主色系：背景 #111111 / 卡片 #1E1E1E / 文字 #E8E8E8
*   漲跌色：上漲 #3ECF6E / 下跌 #E05A5A / 平盤 #777777
*   圓角：卡片 12dp / Badge 4dp

**2.2 首頁結構**
------------

**區塊**

**內容**

**格數**

**數據源**

頂部狀態列

「市場總覽」標題 + 「自動更新中」狀態點

—

—

大盤 / 美國

道瓊工業、NASDAQ、S&P 500、費半 SOXX、台積電 ADR（TSM）

5格（2欄Grid）

Finnhub

大盤 / 日本

日經 225（^N225）

1 格

Yahoo Finance v7

大盤 / 台灣

上市指數（^TWII）/ 上櫃指數（^TPEXCD）/ 台指期 TX（TXF1=F）

3格（2欄Grid）

Yahoo Finance v7

台股自選

用戶自選台股個股，2欄Grid，含上市/上櫃 Badge

動態

Yahoo Finance v7

美股自選

用戶自選美股個股，2欄Grid，含 US Badge

動態

Finnhub

底部資訊列

最後更新時間 + 「數據延遲最多 5 分鐘」

—

—

廣告列（Banner）

AdMob Banner，固定高度 50dp，置於導覽列上方

—

AdMob

底部導覽列

總覽 / 時區 / 自選管理 / 設定，4個Tab

—

—

**2.3 卡片規格**
------------

*   第 1 行：股票代號（粗體）+ 市場 Badge
*   第 2 行：公司 / 指數名稱（細字，灰色）
*   第 3 行：現價（大字，白色）
*   第 4 行：漲跌額 + 漲跌幅百分比 Badge（漲綠底 / 跌紅底 / 平灰底）

_\* 大盤指數卡片無代號行，直接顯示指數名稱。_

**2.4 底部版面規範（v1.1）**
--------------------

**⚠ 廣告緊鄰可點擊 UI 元件違反 AdMob 政策，輕則廣告失效，重則帳號停權。**

正確版面層次（bottomBar 由上至下）：

*   AdMob Banner 廣告（固定 50dp）
*   NavigationBar 導覽列

Scaffold 實作規範：

Scaffold(

bottomBar = {

Column {

BannerAdComposable(Modifier.fillMaxWidth().height(50.dp))

NavigationBar { /\* 4 tabs \*/ }

}

}

) { paddingValues ->

LazyColumn(

contentPadding = PaddingValues(

bottom = paddingValues.calculateBottomPadding() + 8.dp

)

) { /\* 卡片內容 \*/ }

}

_\* Scaffold paddingValues 已含廣告 + 導覽列總高度，LazyColumn 底部卡片不會被遮擋。_

**3\. 數據源規格（定稿）**
=================

**3.1 主要數據源**
-------------

**數據類型**

**主要來源**

**備援**

**全球**

**Symbol 範例**

美股指數 + 個股 + TSM

Finnhub API

Yahoo v7

Yes

^DJI, NVDA, TSM

台股指數 + 個股

Yahoo Finance v7

Finnhub

Yes

^TWII, 2330.TW

上櫃指數 + 個股

Yahoo Finance v7

—

Yes

^TPEXCD, 3293.TW

台指期 TX 近月

Yahoo Finance v7

—

Yes

TXF1=F

日經 225

Yahoo Finance v7

Finnhub

Yes

^N225

**3.2 Yahoo Finance v7 API**
----------------------------

GET https://query1.finance.yahoo.com/v7/finance/quote

fields 參數：regularMarketPrice / regularMarketChange / regularMarketChangePercent / regularMarketTime / marketState / shortName

*   全球可存取，免費，無需 API Key（加 User-Agent header）
*   收盤後 regularMarketPrice 保留當日收盤，不退回昨日數據
*   marketState：REGULAR（盤中）/ CLOSED / PRE / POST / PREPRE / POSTPOST
*   單一請求可查詢多個 symbols（逗號分隔）

**3.3 Finnhub API**
-------------------

GET https://finnhub.io/api/v1/quote?symbol={SYM}&token={KEY}

*   免費方案：每分鐘 60 次呼叫上限
*   回傳欄位：c（現價）/ d（漲跌額）/ dp（漲跌幅%）/ t（時間戳記）
*   需申請免費 API Key：https://finnhub.io

**3.4 Finnhub 節流規範（v1.1 新增）**
-----------------------------

**⚠ 美股大盤 5 格 + 自選最多 20 格 = 單次最大 25 次請求，須節流避免撞限。**

節流策略：

1.  批次分組：每批最多 10 個 Symbol，批次間 delay 1100ms
2.  優先級：固定大盤（5格）優先，自選股延後 2 秒開始
3.  最小刷新間隔：60 秒（含手動下拉刷新）
4.  防抖：不足 60 秒時顯示「剛剛已更新（HH:mm:ss）」，不觸發請求

**情境**

**單次請求數**

**每分鐘上限**

**安全性**

大盤 5 格（固定）

5 次

60 次

OK

大盤 + 自選 20 格

25 次

60 次

OK（單次）

連續兩次刷新（無節流）

50 次

60 次

DANGER

加入 60 秒最小間隔

25 次/分鐘

60 次

OK

**3.5 數據防呆邏輯（v1.1 修正）**
-----------------------

**⚠ v1.0 錯誤：統一用台灣時區比對，美股夜盤後（台灣凌晨）整晚誤判為「數據異常」。**

✓ v1.1 修正：各市場使用本地時區比對，不混用台灣時區。

**市場**

**時區**

**ZoneId**

**台灣對應時間（夏令）**

台股（TSE/OTC）

TST（UTC+8）

Asia/Taipei

13:30 收盤

美股（NYSE/NASDAQ）

EDT（UTC-4）

America/New\_York

隔日 04:00 收盤

日股

JST（UTC+9）

Asia/Tokyo

12:30 收盤

防呆判斷邏輯（Kotlin 偽碼）：

fun validate(regularMarketTime: Long, market: MarketType): DataStatus {

val tz = when(market) {

TAIWAN -> ZoneId.of("Asia/Taipei")

US -> ZoneId.of("America/New\_York") // 自動處理 EST/EDT

JAPAN -> ZoneId.of("Asia/Tokyo")

}

val dataDate = Instant.ofEpochSecond(regularMarketTime).atZone(tz).toLocalDate()

val todayInMkt = LocalDate.now(tz)

return when {

dataDate == todayInMkt -> DataStatus.TODAY

isBeforeOpen(market) -> DataStatus.PREVIOUS\_CLOSE // 盤前

isWeekendOrHoliday(todayInMkt,market)-> DataStatus.PREVIOUS\_CLOSE // 休市

else -> DataStatus.STALE // 真正異常

}

}

**DataStatus**

**UI 顯示**

**說明**

TODAY

正常顯示，無標註

當日數據

PREVIOUS\_CLOSE

卡片底部「上個交易日收盤價」

休市 / 盤前，屬正常

STALE

「數據異常，請稍後重試」

API 異常或真正過期

**4\. 技術架構規格**
==============

**4.1 技術棧**
-----------

**層次**

**技術選型**

**說明**

語言

Kotlin

Google 官方 Android 首選語言

UI

Jetpack Compose

宣告式 UI，AI 生成品質最佳

網路

Retrofit + OkHttp

業界標準

JSON

Moshi

Kotlin 整合佳

本地 DB

Room Database

自選股 + 台股對照表快取 + 報價快取

架構

MVVM + ViewModel

Google 官方推薦

非同步

Kotlin Coroutines + Flow

官方標準

背景排程

WorkManager

省電相容，15 分鐘刷新

廣告

Google AdMob SDK

免費 + 廣告

Min SDK

Android 8.0（API 26）

覆蓋率 95%+

開發工具

Antigravity / VS Code + Android Studio

Vibe Coding 主力

AI 模型

Gemini 3 Pro + Claude

程式生成 + 架構決策

**4.2 Room 資料庫表結構**
-------------------

**資料表**

**主要欄位**

**用途**

watchlist

id / symbol / name / market / sort\_order / is\_fixed

用戶自選股清單

stock\_meta

symbol / name / market / last\_updated

台股對照表快取（v1.1）

quote\_cache

symbol / price / change / change\_pct / market\_time / updated\_at

數據快取供背景更新

_\* stock\_meta 須建立 @Index(value=\["symbol"\]) 和 @Index(value=\["name"\])，確保搜尋效能。_

**4.3 專案目錄結構**
--------------

app/src/main/java/com/stockboard/

ui/ # Compose 畫面元件

home/ # 首頁

manage/ # 自選管理

timezone/ # 時區頁

settings/ # 設定頁

viewmodel/ # ViewModel 層

data/

db/ # Room Entity / DAO / Database

model/ # API 回應資料類別

network/ # Retrofit API Service

worker/ # WorkManager Worker

util/

DateValidator.kt # 防呆多時區邏輯（v1.1）

RateLimiter.kt # Finnhub 節流（v1.1）

MarketStatusHelper.kt # 市場狀態轉換

**5\. Vibe Coding 開發計畫**
========================

**5.1 開發原則**
------------

*   每個 Task 為最小可獨立實作與驗證的單元
*   每個 Task 完成後在 Android Studio 模擬器驗收，通過後才進行下一個
*   每個 Task 附 Prompt 摘要，可直接貼入 Antigravity / VS Code
*   錯誤修復流程：Logcat 錯誤 → 貼給 AI → 修復 → 重驗

**5.2 開發分期總覽**
--------------

**Phase**

**主題**

**Task 數**

**預估**

**里程碑**

Phase 1

專案骨架建立

3

1–2 天

APP 啟動，靜態 UI 完成

Phase 2

大盤數據接入

3

2–3 天

真實大盤指數顯示

Phase 3

自選股功能

4

2–3 天

台股/美股自選股可新增刪除

Phase 4

背景更新 + 節流

2

1–2 天

自動刷新，Rate Limit 安全

Phase 5

數據品質防呆

2

1 天

時區正確，無誤判

Phase 6

廣告整合

2

1 天

AdMob Banner 合規

Phase 7

上架準備

4

2–3 天

上架 Google Play

**Phase 1：專案骨架建立**
------------------

目標：建立可執行 Android 專案，完成首頁靜態 UI（全使用假資料）。

### **Task 1-1：建立 Android 專案**

**項目**

**內容**

目標

建立 Kotlin + Jetpack Compose 專案，模擬器可正常啟動

規格

Package name: com.stockboard｜Min SDK: API 26｜Empty Compose Activity

Dependencies

retrofit2、okhttp3、moshi-kotlin、room-runtime、work-runtime-ktx、coroutines-android、play-services-ads

驗收

模擬器啟動顯示 Compose 畫面，Logcat 無 Error

Prompt 摘要

建立 Android 專案 Package name=com.stockboard，Kotlin + Jetpack Compose，Min SDK 26，在 build.gradle 加入：Retrofit2 + OkHttp3 + Moshi-kotlin + Room-runtime + Work-runtime-ktx + Coroutines-android + Play-services-ads 的 dependencies，並完成基本 MainActivity

### **Task 1-2：首頁靜態 UI — 大盤指數區**

**項目**

**內容**

目標

完成大盤指數靜態 UI，假資料顯示所有指數卡片

規格

LazyColumn 主容器｜2欄 Grid｜美國 5 格 / 日本 1 格 / 台灣 3 格｜深色主題

驗收

所有大盤卡片正確顯示，顏色 / 圓角 / 版型與規格書一致

Prompt 摘要

實作首頁大盤指數靜態 UI：Scaffold + LazyColumn + 2欄 Grid，深色主題（背景#111111/卡片#1E1E1E），分美國（5格:道瓊/NASDAQ/S&P500/SOXX/TSM）/ 日本（1格:日經225）/ 台灣（3格:上市/上櫃/台指期TX）三子區，每卡顯示指數名稱+假資料現價+假資料漲跌幅（綠色）

### **Task 1-3：自選股區 + 底部導覽 + 廣告位**

**項目**

**內容**

目標

完成台股/美股自選卡片區 + 底部 4 Tab + AdMob 廣告佔位（50dp）

規格

Scaffold bottomBar = Column{ BannerAdPlaceholder(50dp) + NavigationBar }｜LazyColumn 使用 paddingValues

驗收

首頁完整可捲動，底部 Tab 可切換，廣告佔位無遮擋，LazyColumn 底部卡片完整可見

Prompt 摘要

在首頁 LazyColumn 下方加入台股自選（3筆假資料）/美股自選（3筆假資料）2欄卡片；Scaffold bottomBar = Column{ Box(height=50dp,background=灰色佔位) + NavigationBar(4 tabs:總覽/時區/自選管理/設定) }；LazyColumn contentPadding bottom = paddingValues.calculateBottomPadding()+8.dp

**Phase 2：大盤數據接入**
------------------

目標：以真實 API 數據取代假資料，首頁顯示即時大盤指數。

### **Task 2-1：Yahoo Finance v7 API 接入**

**項目**

**內容**

目標

呼叫 Yahoo Finance v7，取得台灣（^TWII/^TPEXCD/TXF1=F）與日本（^N225）報價

技術要點

Retrofit YahooFinanceService｜Moshi 解析 quoteResponse.result\[\]｜ViewModel StateFlow｜Compose 訂閱

驗收

台灣大盤區和日本大盤區顯示真實數值，與 Yahoo Finance 網站一致

Prompt 摘要

實作 YahooFinanceApi（Retrofit），GET /v7/finance/quote?symbols=^TWII,^TPEXCD,TXF1=F,^N225&fields=regularMarketPrice,regularMarketChange,regularMarketChangePercent,regularMarketTime,marketState,shortName；Moshi 解析；HomeViewModel StateFlow 更新 Compose UI

### **Task 2-2：Finnhub API 接入（含節流）**

**項目**

**內容**

目標

呼叫 Finnhub 取得道瓊/NASDAQ/S&P500/SOXX/TSM，加入節流保護

技術要點

API Key 存 local.properties｜async 並行查詢｜批次 delay 1100ms｜大盤優先 / 自選延後 2 秒

驗收

美國大盤區顯示 5 個真實數值；連續快速刷新 3 次不出現 429 Error

Prompt 摘要

實作 FinnhubApi（Retrofit），GET /v1/quote?symbol={sym}&token={key}；建立 RateLimiter（minimum interval=60000ms）；HomeViewModel 先呼叫大盤 5 檔，delay(2000) 後呼叫自選股；批次 chunked(10) 每批 delay(1100)

### **Task 2-3：Loading 狀態 + 錯誤處理**

**項目**

**內容**

目標

呼叫中顯示 Loading，失敗顯示錯誤提示，不 crash

技術要點

sealed class UiState { Loading / Success(data) / Error(msg) }｜Compose when(state) 渲染

驗收

關閉網路啟動，卡片顯示錯誤「—」而非空白或 crash；重開網路後恢復正常

Prompt 摘要

HomeViewModel 加入 sealed class UiState；Compose HomeScreen 根據 UiState 顯示 CircularProgressIndicator（Loading）/ 正常卡片（Success）/ 錯誤文字「資料暫時無法取得」（Error）

**Phase 3：自選股功能**
-----------------

目標：實作台股/美股自選股完整 CRUD，含 Room 儲存與台股對照表快取。

### **Task 3-1：Room 資料庫建立（含台股對照表）**

**項目**

**內容**

目標

建立 Room Database，含 watchlist 和 stock\_meta 兩張表

watchlist

id(PK) / symbol / name / market(TSE/OTC/US) / sort\_order / is\_fixed

stock\_meta（v1.1）

symbol(PK) / name / market / last\_updated；加 @Index(symbol) + @Index(name)

驗收

DAO insert/delete/query 正常；APP 重啟後 watchlist 資料保留

Prompt 摘要

建立 AppDatabase（Room），Entity：WatchlistItem（symbol,name,market,sortOrder,isFixed）+ StockMeta（symbol,name,market,lastUpdated，@Entity indices=\[symbol,name\]）；WatchlistDao（insertAll/delete/getAll/getByMarket）+ StockMetaDao（insertAll/search/getLastUpdated）

### **Task 3-2：台股搜尋與新增（24小時快取）**

**項目**

**內容**

目標

實作台股搜尋新增，對照表採 24 小時快取策略

快取邏輯（v1.1）

APP 啟動 -> 查 stock\_meta last\_updated -> 若空或 >24h -> 背景下載 TWSE OpenAPI 並寫入 stock\_meta -> 否則直接用快取

搜尋 SQL

SELECT \* FROM stock\_meta WHERE symbol LIKE :q||'%' OR name LIKE '%'||:q||'%' LIMIT 20

驗收

首次啟動自動下載；24h 內重啟不重複下載；輸入「2330」或「台積」均可找到「台積電」

Prompt 摘要

建立 StockMetaRepository：啟動時 checkAndRefresh()（若 last\_updated 為 null 或 >24h 才呼叫 TWSE OpenAPI 下載），寫入 stock\_meta；SearchViewModel 提供 searchStocks(query: String) 以 Room SQL LIKE 查詢；ManageScreen 顯示搜尋結果 + 確認新增至 watchlist

### **Task 3-3：美股搜尋與新增**

**項目**

**內容**

目標

輸入代號透過 Finnhub Search API 驗證並新增

技術要點

GET /search?q={keyword}｜過濾 type=Common Stock/ETP｜確認後寫入 watchlist（market=US）

驗收

輸入「AAPL」找到「APPLE INC」，新增後首頁美股區即時出現卡片

Prompt 摘要

實作美股搜尋：SearchViewModel.searchUS(query) 呼叫 Finnhub /search，過濾 type in \[Common Stock, ETP\]；ManageScreen 顯示結果列表，點擊後 insert 至 watchlist（market=US）

### **Task 3-4：自選股刪除**

**項目**

**內容**

目標

自選管理頁面實作刪除，滑動或長按觸發

技術要點

SwipeToDismiss Composable｜刪除前 AlertDialog 確認｜確認後 DAO delete + StateFlow 更新

驗收

可刪除任何自選股，首頁即時更新，重啟後確認不再出現

Prompt 摘要

ManageScreen 使用 SwipeToDismiss 包裹每個 WatchlistItem；滑動後彈出 AlertDialog「確定移除 {name}？」；確認呼叫 WatchlistDao.delete() 並 emit 新列表至 StateFlow

**Phase 4：背景更新 + 節流**
---------------------

目標：實作定期自動刷新，前台 5 分鐘 / 背景 15 分鐘，含 Rate Limit 防護。

### **Task 4-1：前台定期刷新（含節流）**

**項目**

**內容**

目標

前台每 5 分鐘刷新，手動刷新有 60 秒最小間隔防抖

技術要點

viewModelScope.launch { repeatOnLifecycle { while(true) { refresh(); delay(5min) } } }｜RateLimiter｜頂部狀態點動畫

驗收

靜置 5 分鐘後數據自動更新；快速連拉 3 次不觸發 429

Prompt 摘要

HomeViewModel 加入 startAutoRefresh()（repeatOnLifecycle + while(true) + delay 300000ms）；加入 RateLimiter.canRefresh()（60秒間隔）；手動刷新觸發時若不可刷新 emit Snackbar「剛剛已更新」；底部顯示 \_lastUpdated StateFlow（HH:mm:ss）

### **Task 4-2：背景排程（WorkManager）**

**項目**

**內容**

目標

APP 背景時 WorkManager 每 15 分鐘刷新並更新 Room 快取

技術要點

PeriodicWorkRequest（15 分鐘）｜StockUpdateWorker 呼叫 API 寫入 quote\_cache｜APP 啟動優先顯示快取

驗收

關閉 APP 15 分鐘後重開，數據已更新（真實裝置驗證）

Prompt 摘要

建立 StockUpdateWorker（CoroutineWorker），doWork() 呼叫 Yahoo Finance v7 + Finnhub，結果寫入 Room quote\_cache；MainActivity.onCreate() 排程 PeriodicWorkRequest（repeatInterval=15分鐘）；HomeViewModel 啟動時先讀取 quote\_cache 顯示，再背景刷新

**Phase 5：數據品質防呆**
------------------

目標：實作各市場時區日期驗證，確保不顯示過期數據或誤判。

### **Task 5-1：數據日期驗證（多時區）**

**項目**

**內容**

目標

依各市場本地時區驗證 regularMarketTime，正確處理台股/美股/日股時差

技術要點

DateValidator.kt｜enum MarketType(TAIWAN/US/JAPAN)｜各市場 ZoneId｜sealed class DataStatus(TODAY/PREVIOUS\_CLOSE/STALE)

關鍵修正（v1.1）

美股用 America/New\_York 時區判斷，避免台灣凌晨誤判美股收盤數據為異常

盤前判斷

台股 08:45 前 / 美股 09:30 ET 前，昨日數據標示 PREVIOUS\_CLOSE，非 STALE

驗收

台股收盤後顯示當日收盤；美股夜盤時間（台灣凌晨）不出現「數據異常」

Prompt 摘要

建立 DateValidator（object）：validate(regularMarketTime: Long, market: MarketType): DataStatus；依 MarketType 選擇 ZoneId 換算本地日期，比對 LocalDate.now(tz)，考慮盤前和週末情境；HomeScreen Composable 根據 DataStatus 在卡片底部顯示標註

### **Task 5-2：市場狀態標註**

**項目**

**內容**

目標

marketState 轉換為中文狀態，時區頁面顯示各市場即時狀態

技術要點

MarketStatusHelper.kt｜REGULAR→「交易中」/ CLOSED/POSTPOST→「已收盤」/ PRE→「盤前」/ POST→「盤後」

驗收

台股開盤中顯示「交易中」，收盤後「已收盤」，美股夜盤「盤後」

Prompt 摘要

實作 MarketStatusHelper.toDisplayStatus(marketState: String): String；TimezoneScreen 顯示三個市場（台股/美股/日股）的名稱/當地時間/開收盤時間/當前狀態列表

**Phase 6：廣告整合**
----------------

目標：整合 AdMob Banner 廣告，符合 AdMob 政策規範。

### **Task 6-1：AdMob 設定與初始化**

**項目**

**內容**

目標

AdMob SDK 初始化，顯示測試廣告

技術要點

Manifest meta-data com.google.android.gms.ads.APPLICATION\_ID｜Application.onCreate() MobileAds.initialize()｜BannerAdComposable（AndroidView 包裝 AdView）

版面（v1.1）

Scaffold bottomBar = Column{ BannerAdComposable(50dp) + NavigationBar }｜LazyColumn paddingValues

驗收

模擬器顯示測試 Banner，Logcat 無廣告 Error，底部卡片不被遮擋

Prompt 摘要

整合 AdMob：Manifest 加入測試 App ID（ca-app-pub-3940256099942544~3347511713）；Application 類別 MobileAds.initialize()；建立 BannerAdComposable（AndroidView 包裝 AdView，adSize=AdSize.BANNER，測試 adUnitId=ca-app-pub-3940256099942544/6300978111）；置於 Scaffold bottomBar 的 NavigationBar 上方

### **Task 6-2：廣告版位上線**

**項目**

**內容**

目標

替換正式 Ad Unit ID，真實裝置驗證

前置

正式 AdMob 廣告單元審核通過（通常 1–3 天）

驗收

真實裝置顯示真實廣告，不遮擋內容，導覽列可正常點擊

Prompt 摘要

將 BannerAdComposable 的 adUnitId 改從 local.properties 讀取（key=admob\_banner\_id），BuildConfig 注入，確保 release build 使用正式 ID

**Phase 7：上架準備**
----------------

目標：完成所有上架前置工作，成功上架 Google Play。

### **Task 7-1：隱私權政策**

**項目**

**內容**

目標

建立並部署隱私權政策頁面（HTTPS）

必要性

AdMob 廣告強制要求；Google Play 上架要求

內容

不收集個人識別資訊；使用 Yahoo Finance/Finnhub 數據（不儲存於伺服器）；Google AdMob 廣告說明

部署

AI 生成 HTML → GitHub Repository → GitHub Pages（免費 HTTPS）

驗收

HTTPS URL 可正常存取，內容完整

Prompt 摘要

生成股市看板 APP 隱私權政策 HTML（繁體中文）：說明 APP 不收集個人識別資訊，使用 Yahoo Finance/Finnhub 取得股市數據（查詢不儲存），使用 Google AdMob 廣告（AdMob 可能依裝置顯示個人化廣告），聯絡方式預留 email 欄位；格式適合 GitHub Pages 部署

### **Task 7-2：APP 簽署（Keystore）**

**項目**

**內容**

目標

生成正式 Keystore，建立 signed release AAB

警告

Keystore 遺失後無法更新同一 APP，必須備份至少 2 個不同位置

技術要點

build.gradle signingConfigs release｜從 local.properties 讀取 storeFile/storePassword/keyAlias/keyPassword

驗收

成功產生 release .aab；Keystore 已備份

Prompt 摘要

在 app/build.gradle 設定 signingConfigs.release（storeFile/storePassword/keyAlias/keyPassword 從 local.properties 讀取），buildTypes.release 套用 signingConfigs.release；提供 keytool 生成 Keystore 指令

### **Task 7-3：商店資產準備**

**項目**

**內容**

目標

準備 Google Play 上架所需全部資產

項目

截圖 2–8 張（最小 1080x1920px）/ APP 圖示 512x512px PNG / 短說明 ≤80字 / 完整說明 ≤4000字

驗收

所有資產符合 Google Play 規格

Prompt 摘要

生成股市看板 APP Google Play 商店文案（繁體中文）：短說明（80字，強調免費/台美股整合/深色介面/自動更新）+ 完整說明（500字，列主要功能/數據來源/特色）；另生成 APP 圖示設計描述（黑底/白色K線圖或股票符號/簡潔）

### **Task 7-4：Google Play Console 提交**

**項目**

**內容**

目標

完成 Play Console 所有設定，提交審核

前置

Play 開發者帳號（USD $25）/ .aab / 隱私權政策 URL / 商店資產

步驟

1.建立應用程式 2.設定商店資料 3.內容評分問卷 4.隱私權政策 URL 5.設定免費 6.上傳.aab 7.提交審核

驗收

Play Console 顯示「正在審核」（通常 1–3 天後上架）

Prompt 摘要

提供 Google Play Console 首次上架完整步驟清單，含各頁面填寫欄位說明 + 常見審核被拒原因（廣告政策/隱私權政策缺失/截圖不符規格）與對策

**6\. 整體驗收檢查表**
===============

**#**

**驗收項目**

**狀態**

1

APP 可在 Android 8.0 以上裝置正常啟動

\[ \]

2

首頁大盤指數：美國 5 格 / 日本 1 格 / 台灣 3 格（含台指期 TX）

\[ \]

3

台積電 ADR（TSM）數據正常顯示

\[ \]

4

台指期 TX（TXF1=F）數據正常顯示

\[ \]

5

台股自選股可新增（上市 + 上櫃）

\[ \]

6

台股對照表 24 小時內不重複下載（v1.1）

\[ \]

7

台股搜尋支援代號與中文名稱

\[ \]

8

美股自選股可新增

\[ \]

9

台股 / 美股自選股可刪除，確認 Dialog 正確顯示

\[ \]

10

自選股清單 APP 重啟後保留

\[ \]

11

數據每 5 分鐘自動刷新

\[ \]

12

連續手動刷新不觸發 Finnhub 429 Error（v1.1）

\[ \]

13

無網路時顯示錯誤狀態，不 crash

\[ \]

14

台股收盤後顯示當日收盤價（非昨日）

\[ \]

15

美股夜盤時段（台灣凌晨）不顯示「數據異常」（v1.1）

\[ \]

16

非交易日 / 盤前正確標註「上個交易日收盤價」（v1.1）

\[ \]

17

AdMob Banner 正常顯示，不遮擋卡片，導覽列可正常點擊（v1.1）

\[ \]

18

隱私權政策 HTTPS URL 可存取

\[ \]

19

Google Play 審核通過，APP 正常上架

\[ \]

_\--- 文件結束 v1.1 ---_