package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "RiskyApp",
    foreignKeys = [
        ForeignKey (
            entity = AppInformation::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"])]
)
data class RiskyApp (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val appId: Int,
    val packageName: String,
    val riskScore: Int
)
