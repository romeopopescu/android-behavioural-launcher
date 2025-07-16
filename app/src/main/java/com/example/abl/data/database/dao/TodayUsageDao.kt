package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.abl.data.database.entity.TodayUsage

@Dao
interface TodayUsageDao {
    @Query("SELECT * FROM today_usage_accumulator")
    suspend fun getAll(): List<TodayUsage>

    @Query("SELECT * FROM today_usage_accumulator WHERE packageName = :packageName")
    suspend fun getUsageForApp(packageName: String): TodayUsage?

    @Upsert
    suspend fun insertOrUpdate(todayUsage: TodayUsage)

    @Query("DELETE FROM today_usage_accumulator")
    suspend fun clearAll()
}