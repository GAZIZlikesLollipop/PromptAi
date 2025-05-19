package com.app.promptai.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.io.File

@Dao
interface ChatDao {
    @Transaction
    @Query("SELECT * FROM chats")
    fun getChats(): Flow<List<ChatCnt>>

    @Query("SELECT * FROM messages WHERE chatOwnerId = :chatId")
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE chats SET name = :newName WHERE chatId = :chatId")
    suspend fun renameChat(chatId: Long,newName: String)

    @Query("UPDATE messages SET content = :newContent WHERE messageId = :msgId AND pictures = :pics")
    suspend fun editMessage(msgId: Long, newContent: String, pics: List<ByteArray>)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)
}