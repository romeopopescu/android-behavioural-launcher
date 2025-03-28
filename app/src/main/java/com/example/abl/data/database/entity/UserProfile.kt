package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserProfile")
data class UserProfile (
    @PrimaryKey(autoGenerate = true)val id: Int,
    val securityQuestion: String,
    val securityAnswer: String
)