package com.example.abl.data.network

import com.google.gson.annotations.SerializedName

data class TrainRequestBody(
    @SerializedName("usage_data") val usageData: List<AppUsageDataApi>,
    val epochs: Int,
    @SerializedName("validation_split") val validationSplit: Float
)

data class AppUsageDataApi(
    val app: String,
    val date: String,
    @SerializedName("first_hour") val firstHour: Int,
    @SerializedName("last_hour") val lastHour: Int,
    @SerializedName("launch_count") val launchCount: Int,
    @SerializedName("total_time_in_foreground") val totalTimeInForeground: Long // in seconds
)

data class TrainingResponseApi(
    val success: Boolean,
    val message: String,
    @SerializedName("model_id") val modelId: String,
    val threshold: Double,
    @SerializedName("training_samples") val trainingSamples: Int
)

data class AnomalyDetectionRequest(
    @SerializedName("usage_data") val usageData: List<AppUsageDataApi>
)

data class AnomalyDetectionResponse(
    val success: Boolean,
    val results: List<AnomalyResult>,
    @SerializedName("overall_risk_level") val overallRiskLevel: String,
    val timestamp: String
)

data class AnomalyResult(
    val app: String,
    val date: String,
    @SerializedName("is_anomaly") val isAnomaly: Boolean,
    @SerializedName("anomaly_score") val anomalyScore: Double,
    @SerializedName("confidence_percent") val confidencePercent: Double,
    @SerializedName("risk_level") val riskLevel: String,
    val details: AnomalyDetails
)

data class AnomalyDetails(
    @SerializedName("first_hour") val firstHour: Int,
    @SerializedName("last_hour") val lastHour: Int,
    @SerializedName("launch_count") val launchCount: Int,
    @SerializedName("total_time_in_foreground") val totalTimeInForeground: Int,
    val threshold: Double
)