package com.app.promptai.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface ApiState {
    @Serializable
    object Initial : ApiState
    @Serializable
    object Loading : ApiState
    @Serializable
    object Success : ApiState
    @Serializable
    object Error : ApiState
}

sealed interface UiState {
    object Initial: UiState
    object Success: UiState
//    object Error: UiState
}