package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.AppUsageRecord
import kotlinx.coroutines.flow.Flow


@Dao
interface AppUsageRecordDao {

    @Insert()
    suspend fun insertAll(usageRecords: List<AppUsageRecord>)

    @Insert()
    suspend fun insert(usageRecord: AppUsageRecord)

    // Get all records for a package within a certain time range (based on when they were recorded)
    @Query("SELECT * FROM app_usage_records WHERE packageName = :packageName AND recordedAt BETWEEN :startTime AND :endTime ORDER BY recordedAt DESC")
    fun getRecordsForPackage(packageName: String, startTime: Long, endTime: Long): Flow<List<AppUsageRecord>>

    // Get all records within a certain time range (based on when they were recorded)
    @Query("SELECT * FROM app_usage_records WHERE recordedAt BETWEEN :startTime AND :endTime ORDER BY recordedAt DESC")
    fun getAllRecords(startTime: Long, endTime: Long): Flow<List<AppUsageRecord>>

    // Example: Get total foreground time for an app over a period of recorded data
    @Query("SELECT SUM(totalTimeInForeground) FROM app_usage_records WHERE packageName = :packageName AND recordedAt BETWEEN :dataRecordStartTime AND :dataRecordEndTime")
    suspend fun getTotalForegroundTimeForPackage(packageName: String, dataRecordStartTime: Long, dataRecordEndTime: Long): Long?

    // Clean up old records
    @Query("DELETE FROM app_usage_records WHERE recordedAt < :timestamp")
    suspend fun deleteRecordsOlderThan(timestamp: Long)
}