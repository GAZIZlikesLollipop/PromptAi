package com.app.promptai.data.model

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.json.Json

class UriListConvertors {
    private val gson = Gson()

    @TypeConverter
    fun fromUriList(uris: List<Uri>?): String? {
        val stringList: List<String>? = uris?.map { it.toString() }
        return stringList?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toUriList(json: String?): List<Uri>? {
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<List<String>>() {}.type
        val stringList: List<String> = gson.fromJson(json, type)
        return stringList.map { it.toUri() }
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