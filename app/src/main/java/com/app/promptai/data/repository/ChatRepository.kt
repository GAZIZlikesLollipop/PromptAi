package com.app.promptai.data.repository

import com.app.promptai.data.database.ChatCnt
import com.app.promptai.data.database.ChatDao
import com.app.promptai.data.database.ChatEntity
import com.app.promptai.data.database.MessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatRepo {
    val messages: (Long) -> Flow<List<MessageEntity>>
    val chats: Flow<List<ChatCnt>>
    suspend fun addChat(chat: ChatEntity)
    suspend fun addInitialChat(chat: ChatEntity): Long
    suspend fun addMessage(message: MessageEntity)
    suspend fun updateChat(chat: ChatEntity)
    suspend fun updateMessage(message: MessageEntity)
    suspend fun deleteChat(chat: ChatEntity)
    suspend fun deleteMessage(message: MessageEntity)
    suspend fun searchChat(chatName: String): List<ChatCnt>
}

class ChatRepository(private val chatDao: ChatDao): ChatRepo {

    override val messages: (Long) -> Flow<List<MessageEntity>> = { chatDao.getMessagesForChat(it) }
    override val chats: Flow<List<ChatCnt>> = chatDao.getChats()

    override suspend fun addChat(chat: ChatEntity){
        chatDao.insertChat(chat)
    }

    override suspend fun addInitialChat(chat: ChatEntity): Long {
        chatDao.insertChat(chat)
        return chat.chatId
    }

    override suspend fun addMessage(message: MessageEntity){
        chatDao.insertMessage(message)
    }

    override suspend fun updateChat(chat: ChatEntity){
        chatDao.updateChat(chat)
    }

    override suspend fun updateMessage(message: MessageEntity) {
        chatDao.updateMessage(message)
    }

    override suspend fun deleteMessage(message: MessageEntity) {
        chatDao.deleteMessage(message)
    }

    override suspend fun deleteChat(chat: ChatEntity){
        chatDao.deleteChat(chat)
    }

    override suspend fun searchChat(chatName: String): List<ChatCnt> {
        return chatDao.searchChatByName(chatName)
    }

}