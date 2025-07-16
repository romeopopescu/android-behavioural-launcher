package com.example.abl.domain.repository

import com.example.abl.domain.model.NormalBehaviourProfile
import kotlinx.coroutines.flow.Flow

interface BehaviouralProfileRepository {
    suspend fun saveNormalBehaviourProfile(profile: NormalBehaviourProfile)
    fun getNormalBehaviourProfile(): Flow<NormalBehaviourProfile?>
    suspend fun clearNormalBehaviourProfile()
}