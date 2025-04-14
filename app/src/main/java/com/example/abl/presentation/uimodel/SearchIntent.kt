package com.example.abl.presentation.uimodel

sealed class SearchIntent {
    data class SearchTextChanged(val text: String): SearchIntent()
    object DeleteSearch : SearchIntent()
}