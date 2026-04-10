package com.stockboard.data.network

import com.stockboard.data.model.TaifexQuoteRequest
import com.stockboard.data.model.TaifexQuoteResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TaifexMisService {
    @POST("fusion/Function/getQuote")
    suspend fun getQuote(@Body request: TaifexQuoteRequest): TaifexQuoteResponse
}
