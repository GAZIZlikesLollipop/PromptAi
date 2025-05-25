package com.app.promptai.presentation

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.promptai.BuildConfig
import com.app.promptai.R
import com.app.promptai.data.database.ChatEntity
import com.app.promptai.data.database.MessageEntity
import com.app.promptai.data.database.SenderType
import com.app.promptai.data.repository.ChatPreferencesRepository
import com.app.promptai.data.repository.ChatRepository
import com.app.promptai.utils.ApiState
import com.app.promptai.utils.UiState
import com.app.promptai.utils.getChatMap
import com.app.promptai.utils.getMimeTypeFromFileUri
import com.app.promptai.utils.uriToBitmap
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
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val chatPreferences: ChatPreferencesRepository,
    private val application: Application
) : ViewModel() {

    private val _apiState: MutableStateFlow<ApiState> = MutableStateFlow(ApiState.Initial)
    val apiState: StateFlow<ApiState> = _apiState.asStateFlow()
//    val chatStates: MutableMap<Long, ApiState> = mutableStateMapOf()
//    val aiChats: MutableMap<Long, Chat> = mutableStateMapOf()

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val picList = mutableStateListOf<Uri>()
    val fileList = mutableStateListOf<Uri>()

    var msgId by mutableIntStateOf(0)

    var isMore by mutableStateOf(false)
    var isEdit by mutableStateOf(false)
    var isOpen by mutableStateOf(false)
    var isWebSearch by mutableStateOf(false)
    var manipulChatId by mutableIntStateOf(0)

    fun switchIsWebSearch(){
        isWebSearch = !isWebSearch
    }

    fun switchIsEdit(){
        isEdit = !isEdit
    }

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

    val currentChatId = chatPreferences.currentChatId.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0
    )

    val messages: StateFlow<List<MessageEntity>> = currentChatId
        .flatMapLatest { chatRepository.messages(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList<MessageEntity>()
        )

    val model = GenerativeModel(
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
                        name = application.getString(R.string.new_chat)
                    )
                )
            }else{
                if(chatsRepo.last().messages.isNotEmpty()) {
                    updateChatId(
                        chatRepository.addInitialChat(
                            ChatEntity(
                                chatId = chatsRepo.size.toLong(),
                                name = application.getString(R.string.new_chat)
                            )
                        )
                    )
                }
            }
            _uiState.value = UiState.Success
        }
    }

    fun newChat(){
        if(apiState.value !is ApiState.Loading) _apiState.value = ApiState.Initial
        viewModelScope.launch {
            if(chatRepository.messages(chats.value.lastIndex.toLong()).first().isNotEmpty()) {
                updateChatId(
                    chatRepository.addInitialChat(
                        ChatEntity(
                            chatId = chats.value.size.toLong(),
                            name = application.getString(R.string.new_chat)
                        )
                    )
                )
            }else{
                updateChatId(chats.value.lastIndex.toLong())
            }
        }
    }

    fun setChatName(chatId: Long){
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatRepository.messages(chatId).first()
            val prompt = "Предыдущий запрос: ${messages[messages.lastIndex-1].content}" +
                    "Предыдущий ответ: ${messages.last().content}" +
                    "придумай одно имя для этого чата без комментариев (язык, на котором будет имя этого чата, зависит от языка твоего предыдущего ответа)"
            val response = model.generateContent(content { text(prompt) })
            chatRepository.editChat(
                ChatEntity(
                    chatId = chatId,
                    name = (response.text ?: "").trim().filter{it.isLetter()||it.isWhitespace()}
                )
            )
        }
    }

    fun sendRequest(
        prompt: String,
        bitmap: List<Bitmap> = emptyList(),
        onResponse: (String) -> Unit,
        files: List<Uri>,
        chatId: Long
    ){
        userPrompt.value = ""
        _apiState.value = ApiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = chat.sendMessage(
                    content {
                        text(prompt)
                        if(bitmap.isNotEmpty()) {
                            bitmap.forEach {
                                image(it)
                            }
                        }

                        if(files.isNotEmpty()){
                            files.forEach { uri ->
                                val file = getMimeTypeFromFileUri(uri)
                                var data: ByteArray? = null
                                val inputStream: InputStream? = application.contentResolver.openInputStream(uri)

                                inputStream?.use { input ->
                                    val byteArrayOutputStream = ByteArrayOutputStream()
                                    val buffer = ByteArray(1024)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        byteArrayOutputStream.write(buffer, 0, bytesRead)
                                    }
                                    data = byteArrayOutputStream.toByteArray()
                                }

                                if(file != null && data != null && file != "application/octet-stream"){
                                    text("file: ${uri.toFile().name}\n")
                                    blob(file, data)
                                }else{
                                    val inputStream = application.contentResolver.openInputStream(uri)
                                    val content = inputStream?.bufferedReader().use { it?.readText() }
                                    if(content != null){
                                        text("file: ${uri.toFile().name}\n${content}")
                                    }
                                }

                            }
                        }
                    }
                )
                val resp = response.text
                if(resp != null) {
                    _apiState.value = ApiState.Success
                    onResponse(resp)
                }else{
                    _apiState.value = ApiState.Error
                    Log.e("API","response is empty")
                }
                if(chats.value[chatId.toInt()].chat.name == application.getString(R.string.new_chat)) {
                    setChatName(chatId)
                }
            } catch (e: Exception) {
                _apiState.value = ApiState.Error
                Log.e("API","${e.localizedMessage}")
            }
        }
    }

    fun regenerateResponse(
        pics: List<Uri> = emptyList(),
        message: MessageEntity,
        prompt: String? = null,
        files: List<Uri> = emptyList()
    ){
        val bitmapList = mutableListOf<Bitmap>()
        val chatId = currentChatId.value
        pics.forEach { uriToBitmap(application,it)?.let { element -> bitmapList.add(element) } }
        viewModelScope.launch {
            chatRepository.updateMessage(
                MessageEntity(
                    messageId = message.messageId+1,
                    content = "",
                    senderType = SenderType.AI,
                    chatOwnerId = chatId
                )
            )
            sendRequest(
                prompt = prompt ?: message.content,
                bitmap = bitmapList,
                onResponse = {
                    viewModelScope.launch {
                        _apiState.value = ApiState.Success
                        chatRepository.updateMessage(
                            MessageEntity(
                                messageId = message.messageId + 1,
                                content = it,
                                chatOwnerId = chatId,
                                senderType = SenderType.AI
                            )
                        )
                    }
                },
                files = files,
                chatId
            )
        }
    }

    fun deleteChat(chat: ChatEntity){
        viewModelScope.launch {
            if(chats.value.size > 1) {
                if(currentChatId.value == chats.value.last().chat.chatId && currentChatId.value.toInt() != 0) {
                    updateChatId(currentChatId.value - 1)
                }
                chatRepository.deleteChat(chat)
            }else{
                chatRepository.messages(manipulChatId.toLong()).first().forEach {
                    chatRepository.deleteMessage(it)
                }
                chatRepository.editChat(
                    ChatEntity(
                        chatId = 0,
                        name = application.getString(R.string.new_chat),
                        creationTimestamp = System.currentTimeMillis(),
                        isFavorite = false
                    )
                )
            }
        }
    }

    fun editChat(chat: ChatEntity){
        viewModelScope.launch {
            chatRepository.editChat(chat)
        }
    }

    fun editMessage(
        text: String,
        pics: List<Uri>,
        files: List<Uri>
    ){
        isEdit = false
        val chatId = currentChatId.value
        viewModelScope.launch {
            val messages = chatRepository.messages(chatId).first()
            messages.forEachIndexed { ind, msg ->
                if(ind > editingMessageId+1){
                    chatRepository.deleteMessage(msg)
                }
            }
            val message = messages[editingMessageId]
            chatRepository.updateMessage(
                MessageEntity(
                    messageId = message.messageId,
                    content = text,
                    chatOwnerId = chatId,
                    senderType = SenderType.USER,
                    pictures = pics,
                    files = files
                )
            )
            regenerateResponse(
                message = message,
                prompt = text,
                pics = pics,
                files = files
            )
        }
    }

    fun getResponse(
        prompt: String,
        pics: List<Uri>,
        files: List<Uri>
    ) {
        val bitmapList = mutableListOf<Bitmap>()
        pics.forEach { uriToBitmap(application,it)?.let { bitmapList.add(it) } }
        val chatId = currentChatId.value
        viewModelScope.launch {
            chatRepository.addMessage(
                MessageEntity(
                    chatOwnerId = chatId,
                    content = prompt,
                    senderType = SenderType.USER,
                    pictures = pics,
                    files = files
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
                bitmap = bitmapList,
                onResponse = {
                    viewModelScope.launch(Dispatchers.IO) {
                        chatRepository.updateMessage(
                            MessageEntity(
                                messageId = chatRepository.messages(chatId).first().last().messageId,
                                content = it,
                                chatOwnerId = chatId,
                                senderType = SenderType.AI
                            )
                        )
                    }
                },
                files = files,
                chatId
            )
        }
    }
}