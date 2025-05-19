package com.example.abl.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abl.data.database.entity.AppInformation
import com.example.abl.domain.repository.AppInformationRepository
import com.example.abl.presentation.uimodel.SearchViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val appInformationRepository: AppInformationRepository,
    @ApplicationContext private val context: Context
): ViewModel() {
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _state = MutableStateFlow(SearchViewState())
    val state = _state.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                appInformationRepository.syncApps()
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    val apps = searchText.combine(appInformationRepository.getAllApps()) { text, apps ->
        val myPackageName = context.packageName
        val filteredApps = apps.filter { it.packageName != myPackageName }
        if (text.isBlank()) {
            filteredApps
        } else {
            filteredApps.filter { app ->
                app.name.contains(text, ignoreCase = true) // ||
//                app.packageName.contains(text, ignoreCase = true)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun deleteSearch() {
        _searchText.value = ""
    }

    fun getAppIcon(packageName: String): Drawable? {
        return appInformationRepository.getAppIcon(packageName)
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            appInformationRepository.launchApp(packageName)
        }
    }
}