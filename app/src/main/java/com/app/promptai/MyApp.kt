package com.app.promptai

import android.app.Application
import androidx.room.Room
import com.app.promptai.data.database.ChatDataBase

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            applicationContext,
            ChatDataBase::class.java,
            "app_database"
        ).build()
    }

    companion object {
        // 3) Глобальная точка доступа
        lateinit var db: ChatDataBase
            private set
    }
}