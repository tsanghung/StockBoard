import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Task 6-2: 從 local.properties 讀取 AdMob Banner Ad Unit ID
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}
val admobBannerId: String = localProps.getProperty("admob_banner_id",
    "ca-app-pub-3940256099942544/6300978111")  // fallback 到測試 ID

android {
    namespace = "com.stockboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stockboard"
        // 4.1 要求：Min SDK Android 8.0 (API 26)
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Task 6-2: 將 AdMob ID 注入 BuildConfig
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true   // Task 6-2: 啟用 BuildConfig 讀取 ADMOB_BANNER_ID
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    // 語言 & UI: Kotlin + Jetpack Compose
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")

    // 架構: MVVM + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // 網路: Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // JSON: Moshi
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")

    // 本地 DB: Room Database
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    ksp("androidx.room:room-compiler:2.6.0")

    // 非同步: Kotlin Coroutines + Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 頁面導航: Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 背景排程: WorkManager
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // 廣告: Google AdMob SDK
    implementation("com.google.android.gms:play-services-ads:22.4.0")

    // 圖片載入: Coil（新聞卡片封面圖）
    implementation("io.coil-kt:coil-compose:2.5.0")

    // 瀏覽器: Chrome Custom Tabs（點擊新聞開啟）
    implementation("androidx.browser:browser:1.7.0")

    // Material Icons Extended（包含 Icons.Filled.Newspaper，core 套件不含此圖示）
    // 不指定版本，由 Compose BOM 統一管理，避免與 compose-ui / material3 版本衝突
    implementation("androidx.compose.material:material-icons-extended")

    // Lifecycle Runtime Compose（提供 collectAsStateWithLifecycle，App 進背景時自動暫停 Flow 訂閱）
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
}
