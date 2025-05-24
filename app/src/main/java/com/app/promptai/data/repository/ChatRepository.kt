package com.app.promptai.data.repository

import android.net.Uri
import com.app.promptai.data.database.ChatCnt
import com.app.promptai.data.database.ChatDao
import com.app.promptai.data.database.ChatEntity
import com.app.promptai.data.database.MessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatRepo {
    suspend fun addInitialChat(chat: ChatEntity): Long
    val messages: (Long) -> Flow<List<MessageEntity>>
    val chats: Flow<List<ChatCnt>>
    suspend fun addChat(chat: ChatEntity)
    suspend fun addMessage(message: MessageEntity)
    suspend fun editChat(chat: ChatEntity)
    suspend fun updateMessage(message: MessageEntity)
    suspend fun deleteChat(chat: ChatEntity)
    suspend fun deleteMessage(message: MessageEntity)
}

class ChatRepository(private val chatDao: ChatDao): ChatRepo {

    override suspend fun addInitialChat(chat: ChatEntity): Long {
        chatDao.insertChat(chat)
        return chat.chatId
    }

    override val messages: (Long) -> Flow<List<MessageEntity>> = { chatDao.getMessagesForChat(it) }
    override val chats: Flow<List<ChatCnt>> = chatDao.getChats()

    override suspend fun addChat(chat: ChatEntity){
        chatDao.insertChat(chat)
    }

    override suspend fun addMessage(message: MessageEntity){
        chatDao.insertMessage(message)
    }

    override suspend fun deleteChat(chat: ChatEntity){
        chatDao.deleteChat(chat)
    }

    override suspend fun editChat(chat: ChatEntity){
        chatDao.updateChat(chat)
    }

    override suspend fun updateMessage(message: MessageEntity) {
        chatDao.updateMessage(message)
    }

    override suspend fun deleteMessage(message: MessageEntity) {
        chatDao.deleteMessage(message)
    }

}