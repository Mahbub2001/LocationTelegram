package com.example.locationtest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var destinationMarker: Marker? = null
    private var selectedLocation: LatLng? = null
    
    companion object {
        const val EXTRA_SELECTED_LAT = "selected_lat"
        const val EXTRA_SELECTED_LNG = "selected_lng"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        setupActionBar()
    }
    
    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Select Destination"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Enable location if permission is granted
        enableMyLocation()
        
        // Set map click listener
        googleMap.setOnMapClickListener { latLng ->
            setDestinationMarker(latLng)
        }
        
        // Set marker click listener for confirmation
        googleMap.setOnMarkerClickListener { marker ->
            if (marker == destinationMarker) {
                confirmDestination()
                true
            } else {
                false
            }
        }
        
        // Move camera to current location or default location
        moveToCurrentLocation()
    }
    
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } ?: run {
                        // Default to a location if current location is not available
                        val defaultLocation = LatLng(37.7749, -122.4194) // San Francisco
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                    }
                }
        }
    }
    
    private fun setDestinationMarker(latLng: LatLng) {
        // Remove existing marker
        destinationMarker?.remove()
        
        // Add new marker
        destinationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Destination")
                .snippet("Tap to confirm this location")
        )
        
        selectedLocation = latLng
        
        Toast.makeText(this, "Tap the marker to confirm destination", Toast.LENGTH_SHORT).show()
    }
    
    private fun confirmDestination() {
        selectedLocation?.let { location ->
            val resultIntent = Intent().apply {
                putExtra(EXTRA_SELECTED_LAT, location.latitude)
                putExtra(EXTRA_SELECTED_LNG, location.longitude)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                    moveToCurrentLocation()
                } else {
                    Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
