# StockBoard 開發原則 (Development Principles)

本專案遵循 Vibe Coding 開發計畫 v1.1 之中設定的 **5.1 開發原則**。
在後續的任何開發過程與程式生成中，所有開發人員與 AI 助理均須嚴格遵守以下守則：

1. **原子化工作單元 (Atomic Tasks):** 
   - 每個 Task 必須限制為「最小可獨立實作」與「獨立驗證」的單元。
   - 不可跨越多個 Task 同時修改基礎架構，以防錯誤蔓延。

2. **嚴格的驗收關卡 (Strict Verification):**
   - 每個 Task 開發完成後，必須在 Android Studio 模擬器 (或本地測試腳本) 驗收。
   - 唯有 100% 通過該 Task 的定義目標後，才可以進行下一個 Task。

3. **Prompt 驅動還原 (Prompt-Driven):**
   - 每個 Task 所需的 Prompt 摘要皆已定義於規格書。
   - 可直接貼入 Antigravity / Cursor / VS Code Copilot 等 AI 工具作為輸入點。

4. **標準錯誤修復流程 (Error Fixing Loop):**
   - 當發生 Crash 等預期外錯誤時，遵循：
   - 擷取 `Logcat 錯誤` → 貼給 AI 分析 → 執行修復 (Fix) → 重新驗收 (Re-test)。
   - 禁止在未了解 Logcat 原理前盲目猜測修改。

---
*版本: v1.1 | 狀態: 強制啟用*
