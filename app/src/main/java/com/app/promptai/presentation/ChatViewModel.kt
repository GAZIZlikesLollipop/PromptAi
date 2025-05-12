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
import com.app.promptai.data.database.ChatEntity
import com.app.promptai.data.database.MessageEntity
import com.app.promptai.data.database.SenderType
import com.app.promptai.data.repository.ChatRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentChatId: MutableStateFlow<Long> = MutableStateFlow(0)
    val currentChatId: StateFlow<Long> = _currentChatId.asStateFlow()

    fun updateChatId(id: Long){
        _currentChatId.value = id
    }

    val chats = chatRepository.chats.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList<ChatCnt>()
    )

    val messages = chatRepository.messages(currentChatId.value).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList<MessageEntity>()
    )
    
    val model = GenerativeModel(
        modelName = "gemini-2.0-flash-lite",
        apiKey = BuildConfig.apiKey
    )
    val chat = model.startChat(
        if(messages.value.isNotEmpty()) {
            listOf(
                content("user") {
                    messages.value.map { if (it.senderType == SenderType.USER) it.content }.joinToString(", ")
                },
                content("model") {
                    messages.value.map { if (it.senderType == SenderType.AI) it.content }.joinToString(", ")
                }
            )
        }else{
            emptyList()
        }
    )

    val userPrompt = MutableStateFlow("")
//    var userPrompt =_userPrompt.asStateFlow()

//    fun changePrompt(prompt: String){
//        _userPrompt.value = prompt
//    }

    init {
        viewModelScope.launch {
            if (chats.value.isEmpty()) {
                chatRepository.addChat(
                    ChatEntity(
                        chatId = 0,
                        name = "New chat"
                    )
                )
            }
        }
    }

    fun newChat(){
        val chatId = (chats.value.lastIndex+1).toLong()
        viewModelScope.launch {
            chatRepository.addChat(
                ChatEntity(
                    chatId = chatId,
                    name = "New chat"
                )
            )
            _currentChatId.value = chatId
        }
    }

    fun sendPrompt(
        bitmap: Bitmap? = null,
        prompt: String,
        chatId: Long
    ) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.addMessage(MessageEntity(
                messageId = if(messages.value.isEmpty()) 0 else (messages.value.lastIndex+1).toLong(),
                chatOwnerId = chatId,
                content = prompt,
                senderType = SenderType.USER
            ))
            try {
                val response = chat.sendMessage(
                    content {
                        if(bitmap != null) {
                            image(bitmap)
                        }
                        text(prompt)
                    }
                )
                response.text?.let {
                    _uiState.value = UiState.Success(it)
                    chatRepository.addMessage(MessageEntity(
                        messageId = if(messages.value.isEmpty()) 0 else (messages.value.lastIndex+1).toLong(),
                        chatOwnerId = chatId,
                        content = it,
                        senderType = SenderType.AI
                    ))
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
                chatRepository.addMessage(MessageEntity(
                    messageId = if(messages.value.isEmpty()) 0 else (messages.value.lastIndex+1).toLong(),
                    chatOwnerId = chatId,
                    content = e.localizedMessage ?: "",
                    senderType = SenderType.AI
                ))
            }
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