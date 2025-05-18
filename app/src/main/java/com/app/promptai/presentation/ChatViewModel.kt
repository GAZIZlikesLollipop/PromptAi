package com.app.promptai.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.promptai.BuildConfig
import com.app.promptai.data.ApiState
import com.app.promptai.data.UiState
import com.app.promptai.data.database.ChatEntity
import com.app.promptai.data.database.MessageEntity
import com.app.promptai.data.database.SenderType
import com.app.promptai.data.repository.ChatPreferencesRepository
import com.app.promptai.data.repository.ChatRepository
import com.app.promptai.utils.getChatMap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val chatPreferences: ChatPreferencesRepository
) : ViewModel() {

    private val _apiState: MutableStateFlow<ApiState> = MutableStateFlow(ApiState.Initial)
    val apiState: StateFlow<ApiState> = _apiState.asStateFlow()

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val currentChatId = chatPreferences.currentChatId.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0
    )

    var isMore by mutableStateOf(false)
    var isEdit by mutableStateOf(false)

    fun switchIsMore(){
        isMore = !isMore
    }

    fun updateChatId(id: Long){
        viewModelScope.launch {
            chatPreferences.editChatId(id)
        }
    }

    val chats = chatRepository.chats.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val chatMap = chatRepository.chats
        .flatMapLatest { getChatMap(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyMap()
        )

    val messages: StateFlow<List<MessageEntity>> = currentChatId
        .flatMapLatest { chatRepository.messages(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList<MessageEntity>()
        )
    
    val model = GenerativeModel(
//        modelName = "gemini-2.0-flash",
        modelName = "gemini-2.5-flash-preview-04-17",
        apiKey = BuildConfig.apiKey
    )

    val chat = model.startChat(
        if(messages.value.isNotEmpty()) {
            listOf(
                content("user") { messages.value.map { if (it.senderType == SenderType.USER) it.content }.joinToString(", ") },
                content("model") { messages.value.map { if (it.senderType == SenderType.AI) it.content }.joinToString(", ") }
            )
        }else{
            emptyList()
        }
    )

    val userPrompt = MutableStateFlow("")
    var editingMessageId by mutableIntStateOf(0)

    init {
        viewModelScope.launch {
            delay(1000)
            val chatsRepo = chatRepository.chats.first()
            if(chatsRepo.isEmpty()) {
                chatRepository.addChat(
                    ChatEntity(
                        chatId = 0,
                        name = "New chat"
                    )
                )
            }else{
                if(chatsRepo.last().messages.isNotEmpty()) {
                    updateChatId(
                        chatRepository.addInitialChat(
                            ChatEntity(
                                chatId = chatsRepo.size.toLong(),
                                name = "New chat"
                            )
                        )
                    )
                }
            }
            _uiState.value = UiState.Success
        }
    }

    fun newChat(){
        _apiState.value = ApiState.Initial
        viewModelScope.launch {
            if(chatRepository.messages(chats.value.lastIndex.toLong()).first().isNotEmpty()) {
                updateChatId(
                    chatRepository.addInitialChat(
                        ChatEntity(
                            chatId = chats.value.size.toLong(),
                            name = "New chat"
                        )
                    )
                )
            }else{
                updateChatId(chats.value.lastIndex.toLong())
            }
        }
    }

    fun setChatName(){
        val chatId = currentChatId.value
        val prompt = "Предыдущий запрос: ${messages.value[messages.value.lastIndex-1].content}" +
                "Предыдущий ответ: ${messages.value.last().content}" +
                "придумай одно имя для этого чата без комментариев (язык, на котором будет имя этого чата, зависит от языка твоего предыдущего ответа)"
        viewModelScope.launch(Dispatchers.IO) {
            val response = model.generateContent(content { text(prompt) })
            chatRepository.renameChat(chatId,(response.text ?: "").trim().filter{it.isLetter()||it.isWhitespace()})
        }
    }

    fun sendRequest(
        prompt: String,
        bitmap: Bitmap? = null,
        onResponse: (String) -> Unit
    ){
        _apiState.value = ApiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = chat.sendMessage(
                    content {
                        if(bitmap != null) {
                            image(bitmap)
                        }
                        text(prompt)
                    }
                )
                val resp = response.text
                if(resp != null) {
                    onResponse(resp)
                }else{
                    _apiState.value = ApiState.Error
                    Log.e("API","response is empty")
                }
                if(messages.value.size <= 2) {
                    setChatName()
                }
            } catch (e: Exception) {
                _apiState.value = ApiState.Error
                Log.e("API","${e.localizedMessage}")
            }
        }
    }

    fun regenerateResponse(
        bitmap: Bitmap? = null,
        message: MessageEntity,
        prompt: String? = null
    ){
        viewModelScope.launch(Dispatchers.IO){
            chatRepository.editMessage(
                messageId = message.messageId+1,
                message = ""
            )
            sendRequest(
                prompt = prompt ?: message.content,
                bitmap = bitmap,
                onResponse = {
                    viewModelScope.launch {
                        _apiState.value = ApiState.Success
                        chatRepository.editMessage(
                            messageId = message.messageId + 1,
                            message = it
                        )
                    }
                }
            )
        }
    }

    fun editMessage(text: String){
        isEdit = false
        viewModelScope.launch {
            messages.value.forEachIndexed { ind, msg ->
                if(ind > editingMessageId+1){
                    chatRepository.deleteMessage(msg)
                }
            }
            val message = messages.value[editingMessageId]
            chatRepository.editMessage(
                messageId = message.messageId,
                message = text
            )
            regenerateResponse(message = message, prompt = text)
        }
    }

    fun getResponse(
        bitmap: Bitmap? = null,
        prompt: String
    ) {
        val chatId = currentChatId.value
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.addMessage(
                MessageEntity(
                    chatOwnerId = chatId,
                    content = prompt,
                    senderType = SenderType.USER
                )
            )
            chatRepository.addMessage(
                MessageEntity(
                    chatOwnerId = chatId,
                    content = "",
                    senderType = SenderType.AI
                )
            )
            sendRequest(
                prompt = prompt,
                bitmap = bitmap,
                onResponse = {
                    viewModelScope.launch(Dispatchers.IO) {
                        _apiState.value = ApiState.Success
                        chatRepository.editMessage(
                            messageId = messages.value.last().messageId,
                            message = it
                        )
                    }
                }
            )
        }
    }
}