package com.example.abl.presentation.uimodel

import com.example.abl.data.database.entity.AppInformation

data class SearchViewState(
    val searchText: String = "",
    val apps: List<AppInformation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
