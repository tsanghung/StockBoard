package com.stockboard.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
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

}
