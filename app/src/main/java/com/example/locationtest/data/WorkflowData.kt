package com.example.locationtest.data

import android.location.Location

data class WorkflowData(
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val message: String = "",
    val destinationLatitude: Double = 0.0,
    val destinationLongitude: Double = 0.0,
    val radiusMeters: Int = 200,
    val isActive: Boolean = false
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun distanceTo(other: LocationPoint): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            this.latitude, this.longitude,
            other.latitude, other.longitude,
            results
        )
        return results[0]
    }
}

enum class WorkflowStatus {
    INACTIVE,
    TRACKING,
    MESSAGE_SENT,
    ERROR
}
