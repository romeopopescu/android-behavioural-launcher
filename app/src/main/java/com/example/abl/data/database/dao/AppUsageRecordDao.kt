package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.AppUsageRecord
import kotlinx.coroutines.flow.Flow


@Dao
interface AppUsageRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usageRecords: List<AppUsageRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usageRecord: AppUsageRecord)

    // Get all records for a package within a certain time range (based on when they were recorded)
    @Query("SELECT * FROM app_usage_records WHERE packageName = :packageName AND recordedAt BETWEEN :startTime AND :endTime ORDER BY recordedAt DESC")
    fun getRecordsForPackage(packageName: String, startTime: Long, endTime: Long): Flow<List<AppUsageRecord>>

    // Get all records within a certain time range (based on when they were recorded)
    // This method is used by BehaviouralProfileManager to get historical data and by ViewModel for display
    // It queries based on recordedAt, which is the timestamp of when the collector processed the data.
    // For accurate profile generation based on daily data, BehaviouralProfileManager might need to query differently or process further.
    @Query("SELECT * FROM app_usage_records WHERE recordedAt BETWEEN :startTime AND :endTime ORDER BY recordedAt DESC")
    fun getAllRecords(startTime: Long, endTime: Long): Flow<List<AppUsageRecord>>

    // New method to get records by the query date they represent
    @Query("SELECT * FROM app_usage_records WHERE queryStartTime >= :rangeStart AND queryStartTime <= :rangeEnd ORDER BY queryStartTime DESC")
    fun getRecordsByQueryDateRange(rangeStart: Long, rangeEnd: Long): Flow<List<AppUsageRecord>>

    // Example: Get total foreground time for an app over a period of recorded data
    @Query("SELECT SUM(totalTimeInForeground) FROM app_usage_records WHERE packageName = :packageName AND recordedAt BETWEEN :dataRecordStartTime AND :dataRecordEndTime")
    suspend fun getTotalForegroundTimeForPackage(packageName: String, dataRecordStartTime: Long, dataRecordEndTime: Long): Long?

    // Clean up old records (e.g., older than X days, based on when they were recorded)
    @Query("DELETE FROM app_usage_records WHERE recordedAt < :timestamp")
    suspend fun deleteRecordsOlderThanRecordedAt(timestamp: Long)

    // New method to delete records within a specific query date range (start of the day)
    @Query("DELETE FROM app_usage_records WHERE queryStartTime >= :rangeStart AND queryStartTime <= :rangeEnd")
    suspend fun deleteRecordsByQueryDateRange(rangeStart: Long, rangeEnd: Long)
}