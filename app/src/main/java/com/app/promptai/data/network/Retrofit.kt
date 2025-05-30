package com.app.promptai.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Retrofit {
    private const val SEARCH_BASE_URL = "https://www.googleapis.com/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SEARCH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val webSearchApiService: WebSearchApiService by lazy {
        retrofit.create(WebSearchApiService::class.java)
    }
}