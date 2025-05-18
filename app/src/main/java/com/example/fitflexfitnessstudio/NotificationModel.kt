package com.example.fitflexfitnessstudio

import java.util.Date

data class NotificationModel(
    val icon: Int,
    val title: String,
    val message: String,
    val timestamp: Date? = null
)