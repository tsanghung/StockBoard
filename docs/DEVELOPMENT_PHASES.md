# StockBoard 開發分期總覽 (Development Phases)

根據 Vibe Coding 開發計畫 v1.1 的 **5.2 開發分期總覽**，以下為本專案的各階段里程碑與 Task 追蹤清單。
每完成一個 Task 且通過驗收後，請在此標記 `[x]`。

## Phase 1: 專案骨架建立 (1-2 天)
- 目標：APP 啟動，靜態 UI 完成
- [x] Task 1-1：建立 Android 專案
- [x] Task 1-2：首頁靜態 UI — 大盤指數區
- [x] Task 1-3：自選股區 + 底部導覽 + 廣告位

## Phase 2: 大盤數據接入 (2-3 天)
- 目標：真實大盤指數顯示
- [x] Task 2-1：Yahoo Finance v7 API 接入
- [x] Task 2-2：Finnhub API 接入（含節流）
- [x] Task 2-3：Loading 狀態 + 錯誤處理

## Phase 3: 自選股功能 (2-3 天)
- 目標：台股/美股自選股可新增刪除
- [x] Task 3-1：Room 資料庫建立（含台股對照表）
- [x] Task 3-2：台股搜尋與新增（24小時快取）
- [x] Task 3-3：美股搜尋與新增
- [x] Task 3-4：自選股刪除

## Phase 4: 背景更新 + 節流 (1-2 天)
- 目標：自動刷新，Rate Limit 安全
- [x] Task 4-1：前台定期刷新（含節流）
- [x] Task 4-2：背景排程（WorkManager）

## Phase 5: 數據品質防呆 (1 天)
- 目標：時區正確，無誤判
- [x] Task 5-1：數據日期驗證（多時區）
- [x] Task 5-2：市場狀態標註

## Phase 6: 廣告整合 (1 天)
- 目標：AdMob Banner 合規
- [x] Task 6-1：AdMob 設定與初始化
- [x] Task 6-2：廣告版位上線

## Phase 7: 上架準備 (2-3 天)
- 目標：上架 Google Play
- [ ] Task 7-1：隱私權政策
- [ ] Task 7-2：APP 簽署（Keystore）
- [ ] Task 7-3：商店資產準備
- [ ] Task 7-4：Google Play Console 提交

---
*狀態: 初始化完成*
