package com.example.abl.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileStatusManager @Inject constructor(
    @ApplicationContext context: Context,
){
    private val prefs: SharedPreferences = context.getSharedPreferences("profile_status", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_INITIAL_TRAINING_DONE = "initial_training_done"
    }

    fun isInitialTrainingDone(): Boolean {
        return prefs.getBoolean(KEY_INITIAL_TRAINING_DONE, false)
    }

    fun setInitialTrainingDone() {
        with(prefs.edit()) {
            putBoolean(KEY_INITIAL_TRAINING_DONE, true)
            apply()
        }
    }
}