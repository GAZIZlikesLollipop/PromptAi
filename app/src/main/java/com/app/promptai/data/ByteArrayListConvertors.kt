package com.app.promptai.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class ByteArrayListConvertors {
    @TypeConverter
    fun fromStringList(list: List<ByteArray>?): String? {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<ByteArray>? {
        if (json == null) {
            return null
        }
        val type: Type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }
}