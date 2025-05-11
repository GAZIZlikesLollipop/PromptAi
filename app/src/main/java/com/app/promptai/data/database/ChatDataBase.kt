package com.app.promptai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatEntity::class, MessageEntity::class], version = 1)
abstract class ChatDataBase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}