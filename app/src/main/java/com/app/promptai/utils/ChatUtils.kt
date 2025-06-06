package com.app.promptai.utils

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.promptai.data.database.ChatCnt
import com.app.promptai.data.repository.ChatPreferencesRepository
import com.app.promptai.data.repository.ChatRepository
import com.app.promptai.data.repository.WebSearchRepository
import com.app.promptai.presentation.ChatViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.collections.forEach

fun getChatMap(chats: List<ChatCnt>): Flow<Map<String, List<ChatCnt>>> {

    val map: MutableMap<String, List<ChatCnt>> = mutableStateMapOf()
    val favoriteList: MutableList<ChatCnt> = mutableListOf()
    val todayList: MutableList<ChatCnt> = mutableListOf()
    val lastWeekList: MutableList<ChatCnt> = mutableListOf()
    val lastMonthList: MutableList<ChatCnt> = mutableListOf()
    val thisYearList: MutableList<ChatCnt> = mutableListOf()
    val olderChatsList: MutableList<ChatCnt> = mutableListOf()

    chats.forEach {
        val time = Instant.ofEpochMilli(it.chat.creationTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime().toLocalDate()
        val recent = time >= LocalDate.now().minusDays(2)
        val lastWeek = !recent && time >= LocalDate.now().minusWeeks(1)
        val lastMonth = !recent && !lastWeek && time >= LocalDate.now().minusDays(30)
        val thisYear = !recent && !lastWeek && !lastMonth && time.year == LocalDate.now().year
        when {
            it.chat.isFavorite -> {
                favoriteList.add(it)
                map["Favorites"] = favoriteList
            }
            recent -> {
                todayList.add(it)
                map["Recent"] = todayList
            }

            lastWeek -> {
                lastWeekList.add(it)
                map["Last week"] = lastWeekList
            }

            lastMonth -> {
                lastMonthList.add(it)
                map["Previous 30 days"] = lastMonthList
            }

            thisYear -> {
                thisYearList.add(it)
                map["This year"] = thisYearList
            }

            else -> {
                olderChatsList.add(it)
                map["Older chats"] = olderChatsList
            }
        }
    }

    return flowOf(map.toMap())
}

class ChatViewModelFactory(
    private val chatRepository: ChatRepository,
    private val chatPreferences: ChatPreferencesRepository,
    private val application: Application,
    private val webSearchRepository: WebSearchRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository,chatPreferences,application,webSearchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

fun bytesToString(bytes: Long): String {
    val byte = when{
        bytes < 1025 -> bytes / 1024.0
        bytes < (1024 * 1024) + 1 -> bytes / (1024.0*1024.0)
        bytes < (1024 * 1024 * 1024) + 1 -> bytes / (1024.0*1024.0*1024.0)
        else -> bytes / (1024.0*1024.0*1024.0*1024.0)
    }

    val finaleBytes = String.format(Locale.getDefault(),"%.2f",byte)

    val finalResp = when {
        bytes < 1025 -> "$finaleBytes KB"
        bytes < (1024 * 1024) + 1 -> "$finaleBytes MB"
        bytes < (1024 * 1024 * 1024) + 1 -> "$finaleBytes GB"
        else -> "$finaleBytes TB"
    }

    return finalResp
}