package com.example.abl.data.database.entity

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Stable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Stable
@Entity(tableName = "AppInformation")
data class AppInformation(
    @PrimaryKey(autoGenerate = true) val appId: Int,
    val name: String,
    val packageName: String,
    val icon: Int//make it int to store the resource id R.drawable.icon
)