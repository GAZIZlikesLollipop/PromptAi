package com.app.promptai

import android.app.Application
import androidx.room.Room
import com.app.promptai.data.database.ChatDataBase

lateinit var db: ChatDataBase
class MyApp: Application(){
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            applicationContext,
            ChatDataBase::class.java,
            "chat_database"
        ).build()
    }
}