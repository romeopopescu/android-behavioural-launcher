package com.example.abl.data

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Stable
import java.util.UUID

@Stable
data class AppInformation(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val id: String = UUID.randomUUID().toString()
) {
    fun doesMatchSearchQuery(query: String): Boolean {
        val matchingCombinations = listOf(
            name,
            "${name.first()}"
        )
        return matchingCombinations.any {
            it.contains(query, ignoreCase = true)
        }
    }
}