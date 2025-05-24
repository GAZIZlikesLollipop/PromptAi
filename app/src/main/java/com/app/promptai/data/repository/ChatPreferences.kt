package com.app.promptai.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.app.promptai.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ChatPreferences {
    val currentChatId: Flow<Long>
    suspend fun editChatId(chatID: Long)
}

class ChatPreferencesRepository(context: Context): ChatPreferences{
    val dataStore = context.dataStore
    private companion object {
        val CURRENT_CHAT_ID_KEY = longPreferencesKey("current_chat_id")
    }

    override val currentChatId: Flow<Long> = dataStore.data.map {
        it[CURRENT_CHAT_ID_KEY] ?: 0
    }

    override suspend fun editChatId(chatID: Long) {
        dataStore.edit {
            it[CURRENT_CHAT_ID_KEY] = chatID
        }
    }
}