# StockBoard 上架 Google Play 計劃

## Context
StockBoard 是一個 Android 股票自選股 App，收費模式為完全免費（靠 AdMob 廣告收益），目標上架全球。
目前程式碼已完整，但缺少：Google Play 開發者帳號、Release 簽章設定、App Bundle 打包。
需要完成帳號申請、產生 Keystore、設定 Release Build、打包上傳、填寫商店資料，才能送審。

---

## Phase 1：申請 Google Play 開發者帳號（瀏覽器操作）

1. 前往 https://play.google.com/console
2. 用 Google 帳號登入
3. 填寫開發者名稱（對外顯示，例如「Simon Wu」或公司名）
4. 繳交一次性 **$25 美元** 註冊費（信用卡）
5. 完成帳號驗證（可能需要上傳身分證件）

---

## Phase 2：產生 Keystore（簽章憑證）

Keystore 是 App 的數位簽章，**一旦上架後絕對不能遺失，否則永遠無法更新 App。**

在 Android Studio 操作：
1. 選單 → Build → Generate Signed Bundle / APK
2. 選「Android App Bundle」→ Next
3. 點「Create new...」建立新 Keystore
4. 填入：
   - **Key store path**：選一個安全位置，例如 `C:\Users\Simon Wu\stockboard-release.jks`
   - **Password**：設定一個強密碼（記下來！）
   - **Alias**：`stockboard`
   - **Key Password**：同上或另設
   - **Validity**：`25`（年）
   - **First and Last Name**：你的名字
5. Next → 選 Release → Finish，Android Studio 會建出 `.aab` 檔案

---

## Phase 3：設定 Release Signing（程式碼，由 Claude 執行）

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/keystore.properties`（加入 .gitignore）

### keystore.properties（不 commit 到 GitHub）
```
storeFile=C:/Users/Simon Wu/stockboard-release.jks
storePassword=<你的密碼>
keyAlias=stockboard
keyPassword=<你的密碼>
```

### app/build.gradle.kts 加入 signingConfig
```kotlin
// 讀取 keystore.properties
val keystoreProps = Properties()
val keystoreFile = rootProject.file("keystore.properties")
if (keystoreFile.exists()) {
    keystoreFile.inputStream().use { keystoreProps.load(it) }
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## Phase 4：打包 Release AAB（由 Claude 執行）

```bash
./gradlew bundleRelease
```

輸出位置：`app/build/outputs/bundle/release/app-release.aab`

---

## Phase 5：在 Google Play Console 建立 App（瀏覽器操作）

1. 進入 https://play.google.com/console
2. 「建立應用程式」
3. 填入：
   - App 名稱：`StockBoard`
   - 預設語言：`繁體中文（台灣）`
   - 應用程式類型：`應用程式`
   - 免費或付費：`免費`
4. 上傳 AAB 檔案（Phase 4 產出的 `.aab`）

---

## Phase 6：填寫商店資訊（瀏覽器操作）

必填項目：

| 項目 | 內容建議 |
|------|---------|
| 簡短說明（80字） | 台股 + 美股自選股即時報價，一覽市場總覽 |
| 完整說明（4000字） | 功能介紹、特色說明 |
| 應用程式圖示 | 512x512 px PNG（用 sell.png 放大） |
| 功能圖示橫幅 | 1024x500 px（可選） |
| 手機截圖 | 至少 2 張，最多 8 張（從模擬器或手機截圖） |
| 類別 | 財經 |
| 內容分級 | 填寫問卷（這個 App 選「適合所有人」） |
| 目標對象 | 18 歲以上 |
| 隱私權政策 URL | 需要一個網頁（可用免費服務產生） |

---

## Phase 7：送審

填完所有必填項目後，點「傳送審查」。
Google 審查時間通常 **3-7 個工作天**。

---

## 注意事項

- `keystore.properties` 加入 `.gitignore`，絕對不上傳 GitHub
- Keystore 檔案 `.jks` 請備份到安全地方（雲端硬碟、隨身碟）
- 隱私權政策頁面是必填，可用 https://www.privacypolicygenerator.info 免費產生
- minSdk=26 代表只支援 Android 8.0 以上裝置（約覆蓋 95%+ 現役 Android 手機）
