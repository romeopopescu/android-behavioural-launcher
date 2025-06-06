package com.example.abl.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

//api calls
interface ApiService {
    @POST("train") 
    suspend fun trainModel(@Body requestBody: TrainRequestBody): Response<TrainingResponseApi>

    @POST("detect")
    suspend fun detectAnomalies(@Body requestBody: AnomalyDetectionRequest): Response<AnomalyDetectionResponse>
} 