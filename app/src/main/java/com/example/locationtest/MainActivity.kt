package com.example.locationtest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.locationtest.data.WorkflowData
import com.example.locationtest.data.WorkflowStatus
import com.example.locationtest.network.TelegramService
import com.example.locationtest.utils.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    
    private lateinit var preferenceManager: PreferenceManager
    
    // UI Elements
    private lateinit var etBotToken: TextInputEditText
    private lateinit var etChatId: TextInputEditText
    private lateinit var etMessage: TextInputEditText
    private lateinit var btnSelectDestination: MaterialButton
    private lateinit var tvSelectedLocation: TextView
    private lateinit var sliderRadius: Slider
    private lateinit var tvRadiusValue: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnStartTracking: MaterialButton
    private lateinit var btnStopTracking: MaterialButton
    private lateinit var btnTestTelegram: MaterialButton
    
    // Data
    private var destinationLatitude: Double = 0.0
    private var destinationLongitude: Double = 0.0
    
    // Activity result launcher for map selection
    private val mapSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                destinationLatitude = data.getDoubleExtra(MapActivitySimple.EXTRA_SELECTED_LAT, 0.0)
                destinationLongitude = data.getDoubleExtra(MapActivitySimple.EXTRA_SELECTED_LNG, 0.0)
                updateLocationDisplay()
            }
        }
    }
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.all { it.value }
        if (!allPermissionsGranted) {
            Toast.makeText(this, "Location permissions are required for this app to work", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        preferenceManager = PreferenceManager(this)
        
        setupViews()
        setupListeners()
        loadSavedData()
        updateUI()
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        requestPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
    
    private fun setupViews() {
        etBotToken = findViewById(R.id.et_bot_token)
        etChatId = findViewById(R.id.et_chat_id)
        etMessage = findViewById(R.id.et_message)
        btnSelectDestination = findViewById(R.id.btn_select_destination)
        tvSelectedLocation = findViewById(R.id.tv_selected_location)
        sliderRadius = findViewById(R.id.slider_radius)
        tvRadiusValue = findViewById(R.id.tv_radius_value)
        tvStatus = findViewById(R.id.tv_status)
        btnStartTracking = findViewById(R.id.btn_start_tracking)
        btnStopTracking = findViewById(R.id.btn_stop_tracking)
        btnTestTelegram = findViewById(R.id.btn_test_telegram)
    }
    
    private fun setupListeners() {
        btnSelectDestination.setOnClickListener {
            val intent = Intent(this, MapActivitySimple::class.java)
            mapSelectionLauncher.launch(intent)
        }
        
        sliderRadius.addOnChangeListener { _, value, _ ->
            tvRadiusValue.text = "${value.toInt()} meters"
        }
        
        btnStartTracking.setOnClickListener {
            startTracking()
        }
        
        btnStopTracking.setOnClickListener {
            stopTracking()
        }
        
        btnTestTelegram.setOnClickListener {
            testTelegramMessage()
        }
    }
    
    private fun loadSavedData() {
        val workflowData = preferenceManager.getWorkflowData()
        
        etBotToken.setText(workflowData.telegramBotToken)
        etChatId.setText(workflowData.telegramChatId)
        etMessage.setText(workflowData.message)
        sliderRadius.value = workflowData.radiusMeters.toFloat()
        tvRadiusValue.text = "${workflowData.radiusMeters} meters"
        
        if (workflowData.destinationLatitude != 0.0 && workflowData.destinationLongitude != 0.0) {
            destinationLatitude = workflowData.destinationLatitude
            destinationLongitude = workflowData.destinationLongitude
            updateLocationDisplay()
        }
    }
    
    private fun updateLocationDisplay() {
        if (destinationLatitude != 0.0 && destinationLongitude != 0.0) {
            tvSelectedLocation.text = "Lat: ${String.format("%.6f", destinationLatitude)}, " +
                    "Lng: ${String.format("%.6f", destinationLongitude)}"
        } else {
            tvSelectedLocation.text = "No destination selected"
        }
    }
    
    private fun updateUI() {
        val status = preferenceManager.getWorkflowStatus()
        val workflowData = preferenceManager.getWorkflowData()
        
        when (status) {
            WorkflowStatus.INACTIVE -> {
                tvStatus.text = "Status: Inactive"
                btnStartTracking.isEnabled = true
                btnStopTracking.isEnabled = false
            }
            WorkflowStatus.TRACKING -> {
                tvStatus.text = "Status: Tracking Location"
                btnStartTracking.isEnabled = false
                btnStopTracking.isEnabled = true
            }
            WorkflowStatus.MESSAGE_SENT -> {
                tvStatus.text = "Status: Message Sent Successfully"
                btnStartTracking.isEnabled = true
                btnStopTracking.isEnabled = false
                // Reset workflow status after display
                preferenceManager.setWorkflowStatus(WorkflowStatus.INACTIVE)
            }
            WorkflowStatus.ERROR -> {
                tvStatus.text = "Status: Error occurred"
                btnStartTracking.isEnabled = true
                btnStopTracking.isEnabled = false
                // Reset workflow status after display
                preferenceManager.setWorkflowStatus(WorkflowStatus.INACTIVE)
            }
        }
    }
    
    private fun startTracking() {
        if (!validateInputs()) {
            return
        }
        
        val workflowData = WorkflowData(
            telegramBotToken = etBotToken.text.toString().trim(),
            telegramChatId = etChatId.text.toString().trim(),
            message = etMessage.text.toString().trim(),
            destinationLatitude = destinationLatitude,
            destinationLongitude = destinationLongitude,
            radiusMeters = sliderRadius.value.toInt(),
            isActive = true
        )
        
        preferenceManager.saveWorkflowData(workflowData)
        preferenceManager.setWorkflowStatus(WorkflowStatus.TRACKING)
        
        LocationTrackingService.startService(this)
        
        updateUI()
        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopTracking() {
        val workflowData = preferenceManager.getWorkflowData().copy(isActive = false)
        preferenceManager.saveWorkflowData(workflowData)
        preferenceManager.setWorkflowStatus(WorkflowStatus.INACTIVE)
        
        LocationTrackingService.stopService(this)
        
        updateUI()
        Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun testTelegramMessage() {
        if (!validateTelegramConfig()) {
            return
        }
        
        lifecycleScope.launch {
            try {
                val result = TelegramService.sendMessage(
                    etBotToken.text.toString().trim(),
                    etChatId.text.toString().trim(),
                    "Test message from Location Tracker"
                )
                
                if (result.isSuccess) {
                    Toast.makeText(this@MainActivity, "Test message sent successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to send test message: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        if (!validateTelegramConfig()) {
            return false
        }
        
        if (destinationLatitude == 0.0 || destinationLongitude == 0.0) {
            Toast.makeText(this, "Please select a destination on the map", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun validateTelegramConfig(): Boolean {
        val botToken = etBotToken.text.toString().trim()
        val chatId = etChatId.text.toString().trim()
        
        if (botToken.isBlank()) {
            Toast.makeText(this, "Please enter Telegram Bot Token", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Validate bot token format
        if (!botToken.matches("^[0-9]+:[A-Za-z0-9_-]+$".toRegex())) {
            Toast.makeText(this, "Invalid bot token format. Should be like: 123456789:ABC...", Toast.LENGTH_LONG).show()
            return false
        }
        
        if (chatId.isBlank()) {
            Toast.makeText(this, "Please enter Chat ID or Username", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Validate chat ID format (numeric or @username)
        if (!chatId.matches("^(@[a-zA-Z0-9_]+|[0-9-]+)$".toRegex())) {
            Toast.makeText(this, "Invalid chat ID format. Use numeric ID or @username", Toast.LENGTH_LONG).show()
            return false
        }
        
        if (etMessage.text.isNullOrBlank()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>().apply {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}