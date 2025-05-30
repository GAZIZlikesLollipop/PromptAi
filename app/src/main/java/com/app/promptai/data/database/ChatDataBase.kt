package com.app.promptai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.promptai.data.model.ApiStateConverter
import com.app.promptai.data.model.UriListConvertors

@Database(entities = [ChatEntity::class, MessageEntity::class], version = 1)
@TypeConverters(UriListConvertors::class,ApiStateConverter::class)
abstract class ChatDataBase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}