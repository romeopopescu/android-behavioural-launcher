package com.example.abl.data.database.model

import androidx.room.TypeConverter

class ProfileTypeConverters {
    @TypeConverter
    fun fromStringSet(stringSet: Set<String>?): String? {
        return stringSet?.joinToString(",")
    }

    @TypeConverter
    fun toStringSet(string: String?): Set<String>? {
        return string?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
    }

    @TypeConverter
    fun fromIntSet(intSet: Set<Int>?): String? {
        return intSet?.joinToString(",") { it.toString() }
    }

    @TypeConverter
    fun toIntSet(string: String?): Set<Int>? {
        return string?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
    }
} 