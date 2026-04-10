package com.stockboard.data.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val yahooFinanceService: YahooFinanceService by lazy {
        Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YahooFinanceService::class.java)
    }

    val finnhubService: FinnhubService by lazy {
        Retrofit.Builder()
            .baseUrl("https://finnhub.io/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FinnhubService::class.java)
    }

    val twseService: TwseService by lazy {
        Retrofit.Builder()
            .baseUrl("https://openapi.twse.com.tw/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TwseService::class.java)
    }

    val twseMisService: TwseMisService by lazy {
        Retrofit.Builder()
            .baseUrl("https://mis.twse.com.tw/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TwseMisService::class.java)
    }

    val yahooChartService: YahooChartService by lazy {
        Retrofit.Builder()
            .baseUrl("https://query2.finance.yahoo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YahooChartService::class.java)
    }

    val tpexService: TpexService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.tpex.org.tw/openapi/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TpexService::class.java)
    }

    val twelveDataService: TwelveDataService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.twelvedata.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TwelveDataService::class.java)
    }

    // 用於 Taifex 的 Cookie 管理器（接受所有 cookie，模擬瀏覽器行為）
    private val taifexCookieStore = mutableMapOf<String, List<Cookie>>()
    private val taifexCookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            taifexCookieStore[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return taifexCookieStore[url.host] ?: emptyList()
        }
    }

    private val taifexClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .cookieJar(taifexCookieJar)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .header("Referer", "https://mis.taifex.com.tw/")
                .header("Origin", "https://mis.taifex.com.tw")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8")
                .method(original.method, original.body)
                .build()
            val response = chain.proceed(request)
            val bodyString = response.body?.string() ?: ""

            Log.d("TaifexRaw", "status=${response.code} firstChar='${bodyString.trimStart().firstOrNull()}' preview=${bodyString.take(120)}")

            // 加入防呆檢查：若收到 HTML 則直接拋出例外，避免進入 Moshi 解析
            if (bodyString.trimStart().startsWith("<")) {
                throw java.io.IOException("Taifex API 回傳 HTML 錯誤網頁，可能遭到防護機制攔截或 Cookie 遺失。")
            }

            response.newBuilder()
                .body(bodyString.toResponseBody(response.body?.contentType()))
                .build()
        }
        .build()

    /** 連線預熱：對首頁發 GET 請求，讓伺服器設定 Session Cookie */
    fun warmupTaifex() {
        try {
            val request = Request.Builder()
                .url("https://mis.taifex.com.tw/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .get()
                .build()
            taifexClient.newCall(request).execute().use { resp ->
                val cookies = taifexCookieJar.loadForRequest("https://mis.taifex.com.tw/".toHttpUrl())
                Log.d("TaifexWarmup", "warmup status=${resp.code} cookieCount=${cookies.size}")
            }
        } catch (e: Exception) {
            Log.e("TaifexWarmup", "warmup failed: ${e.message}")
        }
    }

    val taifexMisService: TaifexMisService by lazy {
        Retrofit.Builder()
            .baseUrl("https://mis.taifex.com.tw/")
            .client(taifexClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TaifexMisService::class.java)
    }
}
