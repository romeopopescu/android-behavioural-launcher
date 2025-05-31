package com.example.abl.presentation.uimodel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap

data class RiskyAppDisplayable(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
    val riskScore: Int
)