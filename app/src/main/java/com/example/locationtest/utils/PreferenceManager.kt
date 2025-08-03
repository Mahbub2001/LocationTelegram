package com.example.locationtest.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.locationtest.data.WorkflowData
import com.example.locationtest.data.WorkflowStatus
import com.google.gson.Gson

class PreferenceManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "LocationTestPrefs", Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    
    companion object {
        private const val KEY_WORKFLOW_DATA = "workflow_data"
        private const val KEY_WORKFLOW_STATUS = "workflow_status"
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CHAT_ID = "chat_id"
        private const val KEY_MESSAGE = "message"
        private const val KEY_DESTINATION_LAT = "destination_lat"
        private const val KEY_DESTINATION_LNG = "destination_lng"
        private const val KEY_RADIUS = "radius"
        private const val KEY_IS_ACTIVE = "is_active"
    }
    
    fun saveWorkflowData(workflowData: WorkflowData) {
        with(sharedPreferences.edit()) {
            putString(KEY_BOT_TOKEN, workflowData.telegramBotToken)
            putString(KEY_CHAT_ID, workflowData.telegramChatId)
            putString(KEY_MESSAGE, workflowData.message)
            putFloat(KEY_DESTINATION_LAT, workflowData.destinationLatitude.toFloat())
            putFloat(KEY_DESTINATION_LNG, workflowData.destinationLongitude.toFloat())
            putInt(KEY_RADIUS, workflowData.radiusMeters)
            putBoolean(KEY_IS_ACTIVE, workflowData.isActive)
            apply()
        }
    }
    
    fun getWorkflowData(): WorkflowData {
        return WorkflowData(
            telegramBotToken = sharedPreferences.getString(KEY_BOT_TOKEN, "") ?: "",
            telegramChatId = sharedPreferences.getString(KEY_CHAT_ID, "") ?: "",
            message = sharedPreferences.getString(KEY_MESSAGE, "") ?: "",
            destinationLatitude = sharedPreferences.getFloat(KEY_DESTINATION_LAT, 0f).toDouble(),
            destinationLongitude = sharedPreferences.getFloat(KEY_DESTINATION_LNG, 0f).toDouble(),
            radiusMeters = sharedPreferences.getInt(KEY_RADIUS, 200),
            isActive = sharedPreferences.getBoolean(KEY_IS_ACTIVE, false)
        )
    }
    
    fun setWorkflowStatus(status: WorkflowStatus) {
        sharedPreferences.edit()
            .putString(KEY_WORKFLOW_STATUS, status.name)
            .apply()
    }
    
    fun getWorkflowStatus(): WorkflowStatus {
        val statusString = sharedPreferences.getString(KEY_WORKFLOW_STATUS, WorkflowStatus.INACTIVE.name)
        return try {
            WorkflowStatus.valueOf(statusString ?: WorkflowStatus.INACTIVE.name)
        } catch (e: IllegalArgumentException) {
            WorkflowStatus.INACTIVE
        }
    }
    
    fun clearWorkflowData() {
        sharedPreferences.edit()
            .clear()
            .apply()
    }
}
