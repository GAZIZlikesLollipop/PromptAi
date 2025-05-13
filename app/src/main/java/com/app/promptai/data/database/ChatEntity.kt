package com.app.promptai.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity("chats")
data class ChatEntity(
    @PrimaryKey val chatId: Long,
    val name: String,
    val creationTimestamp: Long = System.currentTimeMillis()
)

@Entity("messages",
    foreignKeys = [ForeignKey(
        entity = ChatEntity::class,
        parentColumns = ["chatId"],
        childColumns = ["chatOwnerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["chatOwnerId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val chatOwnerId: Long,
    val content: String,
    val senderType: SenderType
)

enum class SenderType { USER, AI }

data class ChatCnt(
    @Embedded val chat: ChatEntity,
    @Relation(
        parentColumn = "chatId",
        entityColumn = "chatOwnerId"
    )
    val messages: List<MessageEntity>
)