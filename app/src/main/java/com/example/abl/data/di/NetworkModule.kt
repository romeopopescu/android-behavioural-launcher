package com.example.abl.data.di

import com.example.abl.data.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        // IMPORTANT: For Android emulator to reach localhost on your machine, use 10.0.2.2
        // If your server is elsewhere, use its actual IP/domain.
        // The user specified 127.0.0.1:8080, which is fine if not testing on emulator directly talking to host.
        // For emulator, use http://10.0.2.2:8080/
        // For physical device on same Wi-Fi, use your computer's local IP: http://<YOUR_COMPUTER_IP>:8080/
        val baseUrl = "http://192.168.30.143:8000/" // Adjust if necessary
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}