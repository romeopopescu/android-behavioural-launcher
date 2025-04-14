package com.example.abl.data.model

sealed class StateResources<out T>{
    data class Success<out T>(val data: T) : StateResources<T>()
    data class Error(val message: String) : StateResources<Nothing>()
    data object Loading : StateResources<Nothing>()
}