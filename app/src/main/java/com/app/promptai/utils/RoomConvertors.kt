package com.app.promptai.utils

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import com.google.ai.client.generativeai.Chat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UriListConvertors {
    private val gson = Gson()

    @TypeConverter
    fun fromUriList(uris: List<Uri>?): String? {
        // 1) Преобразуем каждый Uri в строку
        val stringList: List<String>? = uris?.map { it.toString() }
        // 2) Сериализуем List<String> в JSON
        return stringList?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toUriList(json: String?): List<Uri>? {
        if (json.isNullOrEmpty()) return null
        // 1) Десериализуем JSON в List<String>
        val type = object : TypeToken<List<String>>() {}.type
        val stringList: List<String> = gson.fromJson(json, type)
        // 2) Превращаем каждую строку обратно в Uri
        return stringList.map { it.toUri() }
    }

}

class ChatConvertors {
    private val gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    @TypeConverter
    fun fromChat(chat: Chat?): String? {
        return gson.toJson(chat)
    }

    @TypeConverter
    fun toChat(json: String?): Chat? {
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<Chat>() {}.type
        return gson.fromJson(json, type)
    }

}

class ApiStateConverter {

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }

    @TypeConverter
    fun fromApiState(state: ApiState?): String? {
        return state?.let { json.encodeToString(ApiState.serializer(), it) }
    }

    @TypeConverter
    fun toApiState(jsonString: String?): ApiState? {
        return jsonString?.let { json.decodeFromString(ApiState.serializer(), it) }
    }
}