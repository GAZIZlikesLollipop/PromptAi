package com.app.promptai.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats")
    fun getChats(): Flow<List<ChatCnt>>

    @Query("SELECT * FROM messages WHERE chatOwnerId = :chatId")
    suspend fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Query("UPDATE chats SET name = :newName WHERE chatId = :chatId")
    suspend fun renameChat(chatId: Long,newName: String)
}