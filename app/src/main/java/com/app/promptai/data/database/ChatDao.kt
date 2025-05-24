package com.app.promptai.data.database

import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)
}