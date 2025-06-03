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

    @Query("SELECT * FROM app_usage_records WHERE packageName = :packageName AND recordedAt BETWEEN :startTime AND :endTime ORDER BY recordedAt DESC")
    fun getRecordsForPackage(packageName: String, startTime: Long, endTime: Long): Flow<List<AppUsageRecord>>

    @Query("SELECT * FROM app_usage_records WHERE recordedAt BETWEEN :startTime AND :endTime ORDER BY recordedAt DESC")
    fun getAllRecords(startTime: Long, endTime: Long): Flow<List<AppUsageRecord>>

    @Query("SELECT * FROM app_usage_records WHERE queryStartTime >= :rangeStart AND queryStartTime <= :rangeEnd ORDER BY queryStartTime DESC")
    fun getRecordsByQueryDateRange(rangeStart: Long, rangeEnd: Long): Flow<List<AppUsageRecord>>

    @Query("SELECT SUM(totalTimeInForeground) FROM app_usage_records WHERE packageName = :packageName AND recordedAt BETWEEN :dataRecordStartTime AND :dataRecordEndTime")
    suspend fun getTotalForegroundTimeForPackage(packageName: String, dataRecordStartTime: Long, dataRecordEndTime: Long): Long?

    @Query("DELETE FROM app_usage_records WHERE recordedAt < :timestamp")
    suspend fun deleteRecordsOlderThanRecordedAt(timestamp: Long)

    @Query("DELETE FROM app_usage_records WHERE queryStartTime >= :rangeStart AND queryStartTime <= :rangeEnd")
    suspend fun deleteRecordsByQueryDateRange(rangeStart: Long, rangeEnd: Long)
}