# Network Layer Pruning 實作計劃

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除從 Finnhub/TwelveData 時代遺留的廢棄 API 介面、模型與節流工具，降低維護成本與 APK 體積。

**Architecture:** 刪除無人引用的 Service interface、Data Class、FinnhubFetcher，並清除 ApiClient.kt 中對應的 lazy 屬性。TwseService / TpexService 仍被台股搜尋功能使用，**本次不動**（見下方說明）。

**Tech Stack:** Kotlin、Retrofit、Moshi

---

## ⚠️ 重要說明：TwseService / TpexService 保留原因

使用者需求列出 TwseService / TpexService 為移除項目，但程式碼分析顯示：

- `StockMetaRepository.kt` 呼叫 `ApiClient.twseService.getTseStocks()` 和 `ApiClient.tpexService.getOtcStocks()`
- `SearchViewModel.kt` 使用 `StockMetaRepository` 執行**台股搜尋**（`searchTaiwan()`）

移除這兩個服務會導致台股搜尋功能完全無法運作。**本計劃保留 TwseService / TpexService 及 StockMetaRepository。**

TaifexMisService 檔案本身不存在，無需處理。

---

## 確認安全移除清單

| 項目 | 檔案 | 狀態 |
|------|------|------|
| FinnhubService | `data/network/FinnhubService.kt` | 無任何呼叫端，安全刪除 |
| TwelveDataService | `data/network/TwelveDataService.kt` | 無任何呼叫端，安全刪除 |
| FinnhubQuote | `data/model/FinnhubQuote.kt` | 僅在 FinnhubService 中定義，安全刪除 |
| TwelveDataQuote | `data/model/TwelveDataQuote.kt` | 僅在 TwelveDataService 中定義，安全刪除 |
| FinnhubFetcher | `network/FinnhubFetcher.kt` | 無任何 import，安全刪除 |
| ApiClient.finnhubService | `data/network/ApiClient.kt:30-37` | 移除 lazy 屬性 |
| ApiClient.twelveDataService | `data/network/ApiClient.kt:75-82` | 移除 lazy 屬性 |

**保留（仍在使用）：**
- `FinnhubSearch.kt` / `FinnhubSearchResult` → SearchViewModel + ManageScreen 仍使用
- `TwseService.kt` / `TpexService.kt` → StockMetaRepository 台股搜尋使用
- `TwseMisService.kt` → HomeViewModel 即時報價使用

---

## Task 1：移除廢棄 Service 檔案

**Files:**
- Delete: `app/src/main/java/com/stockboard/data/network/FinnhubService.kt`
- Delete: `app/src/main/java/com/stockboard/data/network/TwelveDataService.kt`

- [ ] **Step 1：刪除 FinnhubService.kt**

```bash
rm "app/src/main/java/com/stockboard/data/network/FinnhubService.kt"
```

- [ ] **Step 2：刪除 TwelveDataService.kt**

```bash
rm "app/src/main/java/com/stockboard/data/network/TwelveDataService.kt"
```

- [ ] **Step 3：確認刪除**

```bash
ls app/src/main/java/com/stockboard/data/network/
```
Expected：不再出現 `FinnhubService.kt` 和 `TwelveDataService.kt`

- [ ] **Step 4：Commit**

```bash
git add -A
git commit -m "chore: remove deprecated FinnhubService and TwelveDataService"
```

---

## Task 2：移除廢棄 Model 檔案

**Files:**
- Delete: `app/src/main/java/com/stockboard/data/model/FinnhubQuote.kt`
- Delete: `app/src/main/java/com/stockboard/data/model/TwelveDataQuote.kt`

- [ ] **Step 1：刪除 FinnhubQuote.kt**

```bash
rm "app/src/main/java/com/stockboard/data/model/FinnhubQuote.kt"
```

- [ ] **Step 2：刪除 TwelveDataQuote.kt**

```bash
rm "app/src/main/java/com/stockboard/data/model/TwelveDataQuote.kt"
```

- [ ] **Step 3：確認刪除**

```bash
ls app/src/main/java/com/stockboard/data/model/
```
Expected：不再出現 `FinnhubQuote.kt` 和 `TwelveDataQuote.kt`

- [ ] **Step 4：Commit**

```bash
git add -A
git commit -m "chore: remove deprecated FinnhubQuote and TwelveDataQuote models"
```

---

## Task 3：移除 FinnhubFetcher.kt

**Files:**
- Delete: `app/src/main/java/com/stockboard/network/FinnhubFetcher.kt`

- [ ] **Step 1：刪除 FinnhubFetcher.kt**

```bash
rm "app/src/main/java/com/stockboard/network/FinnhubFetcher.kt"
```

- [ ] **Step 2：確認 network 目錄是否為空（可一併刪除）**

```bash
ls "app/src/main/java/com/stockboard/network/"
```
Expected：若目錄為空，執行 `rmdir "app/src/main/java/com/stockboard/network/"`

- [ ] **Step 3：Commit**

```bash
git add -A
git commit -m "chore: remove deprecated FinnhubFetcher throttle utility"
```

---

## Task 4：清除 ApiClient.kt 中廢棄的 lazy 屬性

**Files:**
- Modify: `app/src/main/java/com/stockboard/data/network/ApiClient.kt`

移除第 30-37 行的 `finnhubService` 和第 75-82 行的 `twelveDataService`。

- [ ] **Step 1：移除 finnhubService lazy 屬性**

將以下程式碼從 `ApiClient.kt` 中刪除：
```kotlin
val finnhubService: FinnhubService by lazy {
    Retrofit.Builder()
        .baseUrl("https://finnhub.io/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(FinnhubService::class.java)
}
```

- [ ] **Step 2：移除 twelveDataService lazy 屬性**

將以下程式碼從 `ApiClient.kt` 中刪除：
```kotlin
val twelveDataService: TwelveDataService by lazy {
    Retrofit.Builder()
        .baseUrl("https://api.twelvedata.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TwelveDataService::class.java)
}
```

- [ ] **Step 3：確認 ApiClient.kt 乾淨**

```bash
grep -n "finnhub\|twelveData\|FinnhubService\|TwelveData" \
  app/src/main/java/com/stockboard/data/network/ApiClient.kt
```
Expected：無任何輸出

- [ ] **Step 4：Commit**

```bash
git add app/src/main/java/com/stockboard/data/network/ApiClient.kt
git commit -m "chore: remove finnhubService and twelveDataService from ApiClient"
```

---

## Task 5：Build 驗證 + Push

- [ ] **Step 1：執行完整 Debug Build**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```
Expected：`BUILD SUCCESSFUL`

- [ ] **Step 2：確認無殘留引用**

```bash
grep -rn "FinnhubService\|TwelveDataService\|FinnhubQuote\|TwelveDataQuote\|FinnhubFetcher\|finnhubService\|twelveDataService" \
  app/src/main/java --include="*.kt"
```
Expected：無任何輸出（`FinnhubSearchResult` 可以出現，那個保留）

- [ ] **Step 3：Push 到 GitHub**

```bash
git push origin main
```
