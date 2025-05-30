package com.app.promptai.data.network

import com.app.promptai.data.model.SearchApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WebSearchApiService {
    @GET("/customsearch/v1")
    suspend fun webSearch(
        @Query("key") key: String,
        @Query("cx") cx: String,
        @Query("q") query: String,
        @Query("alt") respType: String,
        @Query("num") respCount: Int,
        @Query("filter") filter: String,
        @Query("sort") sort: String,
//        @Query("lr") lang: String,
    ): SearchApiResponse
}