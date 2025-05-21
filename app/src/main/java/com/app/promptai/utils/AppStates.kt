package com.app.promptai.utils

sealed interface ApiState {
    object Initial : ApiState
    object Loading : ApiState
    object Success : ApiState
    object Error : ApiState
}

sealed interface UiState {
    object Initial: UiState
    object Success: UiState
//    object Error: UiState
}