package com.example.abl.utils

object PermissionRiskScorer {
    val permissionScores: Map<String, Int> = mapOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION to 10,
        android.Manifest.permission.ACCESS_COARSE_LOCATION to 8,
        android.Manifest.permission.READ_SMS to 9,
        android.Manifest.permission.RECEIVE_SMS to 7,
        android.Manifest.permission.SEND_SMS to 9,
        android.Manifest.permission.CAMERA to 7,
        android.Manifest.permission.RECORD_AUDIO to 8,
        android.Manifest.permission.READ_CONTACTS to 7,
        android.Manifest.permission.WRITE_CONTACTS to 7,
        android.Manifest.permission.READ_CALL_LOG to 6,
        android.Manifest.permission.WRITE_CALL_LOG to 6,
        android.Manifest.permission.READ_PHONE_STATE to 5,
        android.Manifest.permission.READ_EXTERNAL_STORAGE to 5,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE to 5,
        // MANAGE_EXTERNAL_STORAGE
        // android.Manifest.permission.MANAGE_EXTERNAL_STORAGE to 15,
        // android.Manifest.permission.SYSTEM_ALERT_WINDOW
        // android.Manifest.permission.BIND_DEVICE_ADMIN
        // android.Manifest.permission.REQUEST_INSTALL_PACKAGES
    )
} 