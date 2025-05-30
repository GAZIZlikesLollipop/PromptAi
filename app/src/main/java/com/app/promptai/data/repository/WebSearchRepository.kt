package com.app.promptai.data.repository

import com.app.promptai.BuildConfig
import com.app.promptai.data.model.SearchItem
import com.app.promptai.data.network.WebSearchApiService

class WebSearchRepository(private val webSearchApiService: WebSearchApiService) {
    suspend fun webSearch(query: String): List<SearchItem> {
        return webSearchApiService.webSearch(
            key = BuildConfig.searchKey,
            cx = BuildConfig.searchCx,
            query = query,
            respType = "json",
            respCount = 5,
            filter = "1",
            sort = "date"
        ).items
    }
}