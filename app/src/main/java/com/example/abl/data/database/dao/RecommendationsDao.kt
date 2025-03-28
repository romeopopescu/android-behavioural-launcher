package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.Recommendations

@Dao
interface RecommendationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recommendation: Recommendations)

    @Query("SELECT * FROM Recommendations")
    suspend fun getAllRecommendations(): List<Recommendations>
}