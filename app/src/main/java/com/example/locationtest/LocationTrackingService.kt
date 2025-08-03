package com.example.locationtest

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.example.locationtest.data.LocationPoint
import com.example.locationtest.data.WorkflowData
import com.example.locationtest.data.WorkflowStatus
import com.example.locationtest.network.TelegramService
import com.example.locationtest.utils.PreferenceManager
import kotlinx.coroutines.*

class LocationTrackingService : Service() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var preferenceManager: PreferenceManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var currentWorkflow: WorkflowData? = null
    private var messageSent = false
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "LocationTrackingChannel"
        
        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        currentWorkflow = preferenceManager.getWorkflowData()
        if (currentWorkflow?.isActive == true) {
            startLocationUpdates()
        } else {
            stopSelf()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracking location for Telegram messaging"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracker Active")
            .setContentText("Monitoring your location for destination arrival")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }
    }
    
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 seconds
        ).apply {
            setMinUpdateDistanceMeters(5f) // 5 meters
            setMaxUpdateDelayMillis(15000L) // 15 seconds
        }.build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (securityException: SecurityException) {
            // Handle missing permissions
            stopSelf()
        }
    }
    
    private fun handleLocationUpdate(location: Location) {
        val workflow = currentWorkflow ?: return
        
        if (messageSent) return
        
        val currentLocation = LocationPoint(location.latitude, location.longitude)
        val destination = LocationPoint(workflow.destinationLatitude, workflow.destinationLongitude)
        
        val distance = currentLocation.distanceTo(destination)
        
        if (distance <= workflow.radiusMeters) {
            sendTelegramMessage(workflow)
        }
    }
    
    private fun sendTelegramMessage(workflow: WorkflowData) {
        if (messageSent) return
        
        serviceScope.launch {
            try {
                val result = TelegramService.sendMessage(
                    workflow.telegramBotToken,
                    workflow.telegramChatId,
                    workflow.message
                )
                
                if (result.isSuccess) {
                    messageSent = true
                    preferenceManager.setWorkflowStatus(WorkflowStatus.MESSAGE_SENT)
                    
                    // Update notification
                    val notification = NotificationCompat.Builder(this@LocationTrackingService, CHANNEL_ID)
                        .setContentTitle("Message Sent!")
                        .setContentText("Telegram message sent successfully")
                        .setSmallIcon(R.drawable.ic_location)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                    
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID + 1, notification)
                    
                    // Stop tracking after successful message
                    stopLocationUpdates()
                    stopSelf()
                } else {
                    preferenceManager.setWorkflowStatus(WorkflowStatus.ERROR)
                }
            } catch (e: Exception) {
                preferenceManager.setWorkflowStatus(WorkflowStatus.ERROR)
            }
        }
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
    }
}
