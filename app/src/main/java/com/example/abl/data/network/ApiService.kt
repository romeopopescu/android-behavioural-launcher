package com.example.abl.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// --- Data classes for API request ---
// These are used by ApiService, so they are placed here or in a dedicated model package.
data class AppUsageDataApi(
    @SerializedName("app") val app: String,
    @SerializedName("date") val date: String,
    @SerializedName("first_hour") val firstHour: Int,
    @SerializedName("last_hour") val lastHour: Int,
    @SerializedName("launch_count") val launchCount: Int,
    @SerializedName("total_time_in_foreground") val totalTimeInForeground: Long // e.g., in seconds
)

data class TrainRequestBody(
    @SerializedName("usage_data") val usageData: List<AppUsageDataApi>,
    @SerializedName("epochs") val epochs: Int,
    @SerializedName("validation_split") val validationSplit: Float
)

// --- Retrofit Service Interface ---
interface ApiService {
    @POST("train") 
    suspend fun trainModel(@Body requestBody: TrainRequestBody): Response<Unit> // Assuming a simple success/failure response
} 