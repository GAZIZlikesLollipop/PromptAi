package com.app.promptai.data.repository

import com.app.promptai.data.database.ChatCnt
import com.app.promptai.data.database.ChatDao
import com.app.promptai.data.database.ChatEntity
import com.app.promptai.data.database.MessageEntity
import com.app.promptai.data.database.SenderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface ChatRepo {
    val chats: Flow<List<ChatCnt>>
    val chatMessages: (Long) -> Flow<Pair<List<MessageEntity>,List<MessageEntity>>>
    suspend fun addChat(chat: ChatEntity)
    suspend fun addMessage(message: MessageEntity)
    suspend fun renameChat(chatId: Long,name: String)
    suspend fun deleteChat(chat: ChatEntity)
}

class ChatRepository(private val chatDao: ChatDao): ChatRepo {

    override val chatMessages: (Long) -> Flow<Pair<List<MessageEntity>, List<MessageEntity>>> = { chatId ->
        flow {
            chatDao.getMessagesForChat(chatId).map { messages ->
                val userMessages = mutableListOf<MessageEntity>()
                val aiMessages = mutableListOf<MessageEntity>()
                messages.forEach {
                    if (it.senderType == SenderType.USER) {
                        userMessages.add(it)
                    } else {
                        aiMessages.add(it)
                    }
                }
                Pair(userMessages, aiMessages)
            }
        }
    }

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

    override suspend fun renameChat(chatId: Long,name: String){
        chatDao.renameChat(chatId,name)
    }

}