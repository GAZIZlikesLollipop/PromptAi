package com.app.promptai.data.model

import kotlinx.serialization.Serializable

 @Serializable
data class SearchApiResponse(
    val items: List<SearchItem>
)

@Serializable
data class SearchItem(
    val title: String,
    val snippet: String,
    val link: String
)