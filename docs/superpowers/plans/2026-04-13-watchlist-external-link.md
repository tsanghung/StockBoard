# 實作計畫：自選股卡片點擊開啟外部網頁功能

## 需求說明
使用者在「市場總覽」畫面上的「台股自選」與「美股自選」區域點擊個股卡片時，能夠透過外部瀏覽器開啟對應的 Yahoo 股市資訊網頁。

**具體對應規則：**
- **台股上市股票**（例如 2308 台達電）：開啟網址 `https://tw.stock.yahoo.com/quote/{symbol}.TW`
- **台股上櫃股票**（例如 3293 鈊象）：開啟網址 `https://tw.stock.yahoo.com/quote/{symbol}.TWO`
- *(同場加映)* **美股股票**：開啟網址 `https://finance.yahoo.com/quote/{symbol}`

---

## User Review Required

> [!NOTE]
> 在 `HomeScreen.kt` 中判斷上市與上櫃的依據，會直接採用 `q.badgeText` (判斷其字串值為 "上市" 或是 "上櫃")。這個標籤是在 `HomeViewModel` 的 `fetchTwStockQuotes` 方法裡產生的。
> 同時，實作中會順便把下方的「美股自選」卡片一起加上點擊事件，確保操作體驗一致。

## Proposed Changes

### UI Components

#### [MODIFY] StockCard.kt 
- **變更項目**：在 `StockCard` Composable 函式中新增 `onClick: () -> Unit = {}` 參數。
- **變更項目**：在最外層的 `Card` 元件的 `modifier` 加上 `.clickable { onClick() }` 方法讓卡片產生互動效果。

#### [MODIFY] HomeScreen.kt 
- **變更項目**：在繪製台股自選的地方，替傳遞資料到 `StockCard` 時，寫入 `onClick` 點擊事件。
  - **事件邏輯**：判斷 `q.badgeText`。如果是 `"上櫃"`，則使用後綴 `.TWO`；否則（上市）使用後綴 `.TW`。組裝 URL 並發起 `Intent.ACTION_VIEW` 啟動外部瀏覽器。
- **變更項目**：在繪製美股自選的地方，替傳入的 `StockCard` 同樣加上 `onClick` 點擊事件，對應的網址為美版 Yahoo Finance `https://finance.yahoo.com/quote/${q.symbol}`。

## Verification Plan

### Manual Verification
1. 重建並執行 App，開啟至首頁「市場總覽」。
2. 點擊「台股自選」中的**上櫃股票** (如：3293 鈊象)，確認是否開啟瀏覽器並順利顯示 `https://tw.stock.yahoo.com/quote/3293.TWO`。
3. 點擊「台股自選」中的**上市股票** (如：2308 台達電)，確認是否開啟瀏覽器並順利顯示 `https://tw.stock.yahoo.com/quote/2308.TW`。
4. 點擊「美股自選」中的股票，確認是否順利開啟美國版雅虎財經對應個股頁面。

---

## Known Issues & Future Improvements

> [!WARNING]
> **Edge Case:** 當 `fetchTwStockQuotes` API 呼叫失敗時，`badgeText` 會觸發 fallback 被設定為 `"台股"`：
> `items.map { StockQuoteUiModel(it.symbol, it.name, it.market, "台股") }`
>
> 由於目前 `HomeScreen` 中的判斷邏輯是：若是 `"上櫃"` 則結尾 `.TWO`，否則皆視為 `.TW`。
> 若原本是「上櫃」股票但遭遇 API 失敗，此時點擊將會導向錯誤的 `.TW` URL（例如應為 `3293.TWO` 卻變成 `3293.TW`）。這不會導致 App 崩潰，但會讓連結短暫失效。
>
> **Future Fix:** 這是現有資料結構的限制，短期內可接受。未來若需根治，建議在 `WatchlistItem` 或 `StockQuoteUiModel` 中加入實體的 TSE/OTC (`MarketType`) 欄位，不再單純依賴 `badgeText` 來判斷上市櫃。
