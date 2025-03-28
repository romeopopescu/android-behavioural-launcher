package com.example.abl.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.example.abl.data.database.entity.AppInformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

class SearchViewModel(private val context: Context): ViewModel() {
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val allApps = getInstalledApps(context)
    private val _apps = MutableStateFlow(allApps)

    val apps = searchText.combine(_apps) { text, apps ->
        if (text.isBlank()) {
            apps
        } else {
            apps.filter {
                it.doesMatchSearchQuery(text)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _apps.value
    )

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }
    fun deleteSearch() {
        _searchText.value = ""
    }
}
data class AppInformationTest(
    val name: String,
    val packageName: String,
    val icon: Drawable, //make it int to store the resource id R.drawable.icon
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

fun getInstalledApps(context: Context): List<AppInformationTest> {
    val packageManager = context.packageManager

    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val apps = packageManager.queryIntentActivities(intent, 0).map {
        val appName = it.activityInfo.loadLabel(packageManager).toString()
        val packageName = it.activityInfo.packageName
        val icon = it.activityInfo.loadIcon(packageManager)

        AppInformationTest(appName, packageName, icon)
    }

    return apps.sortedBy { it.name.lowercase() }
}
