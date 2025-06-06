package com.app.promptai.presentation

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.promptai.BuildConfig
import com.app.promptai.R
import com.app.promptai.data.database.ChatEntity
import com.app.promptai.data.database.MessageEntity
import com.app.promptai.data.database.SenderType
import com.app.promptai.data.database.defaultChatName
import com.app.promptai.data.model.ApiState
import com.app.promptai.data.model.UiState
import com.app.promptai.data.repository.ChatPreferencesRepository
import com.app.promptai.data.repository.ChatRepository
import com.app.promptai.data.repository.WebSearchRepository
import com.app.promptai.utils.getChatMap
import com.app.promptai.utils.getMimeTypeFromFileUri
import com.app.promptai.utils.uriToBitmap
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionCallPart
import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
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
    private val chatPreferences: ChatPreferencesRepository,
    private val application: Application,
    private val webSearchRepository: WebSearchRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val picList = mutableStateListOf<Uri>()
    val fileList = mutableStateListOf<Uri>()

    var msgId by mutableIntStateOf(0)

    var isMore by mutableStateOf(false)
    var isEdit by mutableStateOf(false)
    var isOpen by mutableStateOf(false)
    var manipulChatId by mutableIntStateOf(0)
    var isWebSearch by mutableStateOf(false)

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
            emptyList()
        )
    var aiMsg: MessageEntity by mutableStateOf(MessageEntity(0,0,"",SenderType.USER))

    val queryParameterSchema = Schema.str(
        name = "query",
        description = "The search query to use for finding information."
    )

    // Now define your FunctionDeclaration using YOUR FunctionDeclaration's constructor
    val webSearchTool= FunctionDeclaration(
        name = "search_web",
        description = "Searches the web for current information based on a user's query.",
        parameters = listOf(queryParameterSchema), // Pass a List of Schema objects
        requiredParameters = listOf("query") // List of names of required parameters
    )

    val model = GenerativeModel(
        modelName = "gemini-2.5-flash-preview-04-17",
        apiKey = BuildConfig.apiKey,
        tools = listOf(Tool(functionDeclarations = listOf(webSearchTool)))
    )
    val aiChats = mutableStateMapOf<Long,Chat>()

    val userPrompt = MutableStateFlow("")
    var editingMessageId by mutableIntStateOf(0)

    init {
        viewModelScope.launch {
            delay(1000)
            val chatsExp = chatRepository.chats.first()
            if (chatsExp.isEmpty()) {
                chatRepository.addChat(
                    ChatEntity(
                        chatId = 0,
                        name = application.getString(R.string.new_chat)
                    )
                )
                aiChats[0] = model.startChat()
            } else {
                if (chatsExp.last().messages.isNotEmpty()) {
                    updateChatId(
                        chatRepository.addInitialChat(
                            ChatEntity(
                                chatId = chatsExp.size.toLong(),
                                name = application.getString(R.string.new_chat)
                            )
                        )
                    )
                }
            }

            chats.value.forEach { aiChats[it.chat.chatId] = model.startChat() }
            defaultChatName = application.getString(R.string.new_chat)
            _uiState.value = UiState.Success
        }
    }

    val searchChat = MutableStateFlow("")

    val searchChatMap = searchChat
        .flatMapLatest { getChatMap(chatRepository.searchChat(it.trim())) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyMap()
        )

    fun editSearchChat(chat: String){
        searchChat.value = chat
    }

    fun newChat(){
        viewModelScope.launch {
            if(chatRepository.messages(chats.value.lastIndex.toLong()).first().isNotEmpty()) {
                val chatId = chatRepository.addInitialChat(
                    ChatEntity(
                        chatId = chats.value.size.toLong(),
                        name = application.getString(R.string.new_chat)
                    )
                )
                updateChatId(chatId)
                aiChats[chatId] = model.startChat()
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
            chatRepository.updateChat(chats.value[chatId.toInt()].chat.copy(name = (response.text ?: "").trim()))//.filter{it.isLetter()||it.isWhitespace()}))
        }
    }

     fun sendRequest(
        prompt: String,
        bitmap: List<Bitmap> = emptyList(),
        files: List<Uri> = emptyList(),
        onResponse: (String) -> Unit,
        chatId: Long
    ) {
        userPrompt.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.updateChat(chats.value[chatId.toInt()].chat.copy(chatState = ApiState.Loading))
            try {
                val chat = aiChats[chatId] ?: model.startChat().also { aiChats[chatId] = it }

                // Создание начального контента
                val initialContent = content {
                    text(prompt)
                    bitmap.forEach { image(it) }
                    files.forEach { uri ->
                        val mimeType = getMimeTypeFromFileUri(uri)
                        val data = application.contentResolver.openInputStream(uri)?.readBytes()
                        if (mimeType != null && data != null && mimeType != "application/octet-stream") {
                            blob(mimeType, data)
                        } else if (data != null) {
                            val contentText = application.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                            if (contentText != null) {
                                text("file: ${uri.lastPathSegment ?: "unknown"}\n${contentText}")
                            }
                        }
                    }
                }

                // Отправка запроса модели
                var response = chat.sendMessage(initialContent)

                // Обработка вызовов инструментов
                if(isWebSearch) {
                    while (response.candidates.any { it.content.parts.any { part -> part is FunctionCallPart } } == true) {
                        val functionCallPart = response.candidates
                            .flatMap { it.content.parts }
                            .filterIsInstance<FunctionCallPart>()
                            .firstOrNull()

                        if (functionCallPart != null) {
                            val toolName = functionCallPart.name
                            val arguments = functionCallPart.args

                            // Выполнение соответствующего инструмента
                            val toolOutput = when (toolName) {
                                "search_web" -> {
                                    val query = arguments["query"]
                                    if (query != null) {
                                        val results = webSearchRepository.webSearch(query)
                                        results.joinToString("\n") { "${it.title}\n${it.snippet}\n${it.link}" }
                                    } else {
                                        "Invalid query parameter."
                                    }
                                }

                                else -> "Unknown tool: $toolName"
                            }

                            // Отправка результата инструмента обратно модели
                            response = chat.sendMessage(
                                content(role = "user") {
                                   text("search_web response: $toolOutput")
                                }
                            )
                        } else {
                            break
                        }
                    }
                }

                val finalResponse = response.text
                if (finalResponse != null) {
                    chatRepository.updateChat(chats.value[chatId.toInt()].chat.copy(chatState = ApiState.Success))
                    onResponse(finalResponse)
                } else {
                    chatRepository.updateChat(chats.value[chatId.toInt()].chat.copy(chatState = ApiState.Error))
                    Log.e("API", "response is empty")
                }

                if (chats.value[chatId.toInt()].chat.name == application.getString(R.string.new_chat)) {
                    setChatName(chatId)
                }
            } catch (e: Exception) {
                chatRepository.updateChat(chats.value[chatId.toInt()].chat.copy(chatState = ApiState.Error))
                Log.e("API", e.localizedMessage ?: "Error")
            }
        }
    }

    fun regenerateResponse(
        pics: List<Uri> = emptyList(),
        userMsg: MessageEntity,
        aiMsg: MessageEntity,
        prompt: String? = null,
        files: List<Uri> = emptyList()
    ){
        val bitmapList = mutableListOf<Bitmap>()
        val chatId = userMsg.chatOwnerId
        (if(pics.isNotEmpty()) pics else userMsg.pictures).forEach { uriToBitmap(application,it)?.let { element -> bitmapList.add(element) } }
        viewModelScope.launch {
            val msg = chatRepository.messages(chatId).first()
            msg.forEachIndexed { ind, cnt ->
                if(ind > msg.indexOf(aiMsg)){
                    chatRepository.deleteMessage(cnt)
                }
            }
            chatRepository.updateMessage(msg[msg.indexOf(aiMsg)].copy(content = ""))
            sendRequest(
                prompt = prompt ?: userMsg.content,
                bitmap = bitmapList,
                onResponse = {
                    viewModelScope.launch {
                        chatRepository.updateMessage(msg[msg.indexOf(aiMsg)].copy(content = it))
                    }
                },
                files = if(files.isNotEmpty())files else userMsg.files,
                chatId = chatId
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
                chatRepository.updateChat(
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
            chatRepository.updateChat(chat)
        }
    }

    fun editMessage(
        text: String,
        pics: List<Uri>,
        files: List<Uri>,
        aiMsg: MessageEntity
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
                messages[messages.indexOf(message)].copy(
                    content = text,
                    pictures = pics,
                    files = files
                )
            )
            regenerateResponse(
                userMsg = message,
                prompt = text,
                pics = pics,
                files = files,
                aiMsg = aiMsg
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
                        chatRepository.updateMessage(chatRepository.messages(chatId).first().last().copy(content = it))
                    }
                },
                files = files,
                chatId = chatId
            )
        }
    }
}