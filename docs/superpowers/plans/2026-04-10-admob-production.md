# AdMob 正式上線實作計劃

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 將 StockBoard App 的廣告從測試 ID 換成真實 AdMob ID，開始產生廣告收益。

**Architecture:** 程式碼已透過 `local.properties` 注入機制分離 Ad Unit ID，`AndroidManifest.xml` 存放 App ID。帳號申請完成後只需更新這兩個地方即可。

**Tech Stack:** Google AdMob、Android `local.properties`、`BuildConfig`

---

## Task 1：申請 AdMob 帳號（瀏覽器操作）

**需要你自己操作，我無法代勞。**

- [ ] **Step 1：前往 AdMob 官網**

打開瀏覽器，前往：`https://admob.google.com`

- [ ] **Step 2：用 Google 帳號登入**

點右上角「開始使用」或「登入」，選擇你的 Google 帳號。

- [ ] **Step 3：填寫帳號基本資料**

| 欄位 | 填入 |
|------|------|
| 國家/地區 | 台灣 |
| 時區 | 台灣標準時間 (UTC+8) |
| 幣別 | TWD（新台幣）或 USD（美元） |

- [ ] **Step 4：接受使用條款**

勾選同意條款 → 點「建立 AdMob 帳戶」

- [ ] **Step 5：確認帳號建立成功**

看到 AdMob 主控台首頁即完成。

---

## Task 2：在 AdMob 新增 App（瀏覽器操作）

- [ ] **Step 1：進入「應用程式」頁面**

左側選單 → 點「應用程式」→「新增應用程式」

- [ ] **Step 2：選擇平台**

選「Android」

- [ ] **Step 3：選擇上架狀態**

選「**否，尚未在應用程式商店中發佈**」

- [ ] **Step 4：填入 App 資訊**

| 欄位 | 填入 |
|------|------|
| App 名稱 | `StockBoard` |
| Package Name（選填） | `com.stockboard` |

- [ ] **Step 5：取得 AdMob App ID**

建立完成後，頁面會顯示：

```
您的應用程式 ID：ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
```

**把這串 ID 複製下來，等一下要用。**

---

## Task 3：建立 Banner 廣告單元（瀏覽器操作）

- [ ] **Step 1：進入廣告單元頁面**

在剛建立的 App 頁面 → 點「廣告單元」→「新增廣告單元」

- [ ] **Step 2：選擇廣告類型**

選「**橫幅廣告（Banner）**」

- [ ] **Step 3：設定廣告單元名稱**

名稱填：`StockBoard-Banner`

其他設定保持預設即可。

- [ ] **Step 4：取得 Ad Unit ID**

建立完成後，頁面會顯示：

```
您的廣告單元 ID：ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
```

**把這串 ID 複製下來，等一下要用。**

- [ ] **Step 5：把兩個 ID 告訴我**

把以下兩個 ID 貼給我：
1. **AdMob App ID**（含 `~` 符號，從 Task 2 Step 5 取得）
2. **Banner Ad Unit ID**（含 `/` 符號，從本 Task Step 4 取得）

---

## Task 4：更新程式碼（等拿到 ID 後由 Claude 執行）

**Files:**
- Modify: `local.properties`（專案根目錄）
- Modify: `app/src/main/AndroidManifest.xml:29`

- [ ] **Step 1：更新 local.properties**

在 `local.properties` 加入：
```
admob_banner_id=<你的真實 Banner Ad Unit ID>
```

- [ ] **Step 2：更新 AndroidManifest.xml**

將第 29 行：
```xml
android:value="ca-app-pub-3940256099942544~3347511713"
```
改為：
```xml
android:value="<你的真實 AdMob App ID>"
```

- [ ] **Step 3：確認 local.properties 在 .gitignore 中**

```bash
grep "local.properties" .gitignore
```
Expected：看到 `local.properties` 這行（確保 Ad Unit ID 不會上傳 GitHub）

- [ ] **Step 4：Build 確認**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```
Expected：`BUILD SUCCESSFUL`

- [ ] **Step 5：Commit AndroidManifest 變更（不 commit local.properties）**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: replace test AdMob App ID with production ID"
git push origin main
```

---

## Task 5：安裝到手機驗證

- [ ] **Step 1：在 Android Studio 重新 Run**

連接手機 → Android Studio 點 ▶ Run，重新安裝最新版本

- [ ] **Step 2：確認廣告顯示**

打開 App，底部廣告欄位：
- 測試成功前可能顯示空白（AdMob 審核中，正常現象）
- 若顯示「Test Ad」代表 App ID 或 Ad Unit ID 還是測試值，需重新確認

---

## 注意事項

- AdMob 新帳號審核需 **1-3 天**，這段期間廣告可能空白，不代表設定錯誤
- `local.properties` **不會** 被 commit 到 GitHub（已在 .gitignore），廣告 ID 安全
- 真實廣告收益需要真實用戶點擊，安裝初期收益極低屬正常
