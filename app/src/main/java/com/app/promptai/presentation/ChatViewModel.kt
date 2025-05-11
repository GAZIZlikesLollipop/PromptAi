package com.app.promptai.presentation

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.promptai.BuildConfig
import com.app.promptai.data.UiState
import com.app.promptai.data.database.ChatCnt
import com.app.promptai.data.repository.ChatRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val chats = chatRepository.chats.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList<ChatCnt>()
    )

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey
    )

    var userPrompt by mutableStateOf("")

    fun sendPrompt(
        bitmap: Bitmap? = null,
        prompt: String
    ) {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    content {
                        if(bitmap != null) {
                            image(bitmap)
                        }
                        text(prompt)
                    }
                )
                response.text?.let { _uiState.value = UiState.Success(it) }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
            userPrompt = ""
        }
    }
}

class ChatViewModelFactory(private val chatRepository: ChatRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}