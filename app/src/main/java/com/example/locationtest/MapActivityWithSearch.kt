package com.example.locationtest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.locationtest.adapter.SearchResultAdapter
import com.example.locationtest.data.SearchResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class MapActivityWithSearch : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var destinationMarker: Marker? = null
    private var selectedLocation: LatLng? = null
    
    // UI Elements
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var btnCurrentLocation: FloatingActionButton
    private lateinit var btnConfirmLocation: FloatingActionButton
    
    // Search
    private lateinit var searchAdapter: SearchResultAdapter
    private val searchScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var searchJob: Job? = null
    
    companion object {
        const val EXTRA_SELECTED_LAT = "selected_lat"
        const val EXTRA_SELECTED_LNG = "selected_lng"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        
        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        setupViews()
        setupMap()
        setupSearch()
        setupActionBar()
    }
    
    private fun setupViews() {
        etSearch = findViewById(R.id.et_search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        rvSearchResults = findViewById(R.id.rv_search_results)
        btnCurrentLocation = findViewById(R.id.btn_current_location)
        btnConfirmLocation = findViewById(R.id.btn_confirm_location)
        
        // Setup RecyclerView
        searchAdapter = SearchResultAdapter { searchResult ->
            selectSearchResult(searchResult)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = searchAdapter
        
        // Setup button listeners
        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
            hideSearchResults()
        }
        
        btnCurrentLocation.setOnClickListener {
            moveToCurrentLocation()
        }
        
        btnConfirmLocation.setOnClickListener {
            confirmDestination()
        }
    }
    
    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                
                if (query.isNotEmpty()) {
                    btnClearSearch.visibility = View.VISIBLE
                    searchPlaces(query)
                } else {
                    btnClearSearch.visibility = View.GONE
                    hideSearchResults()
                }
            }
        })
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
            hideSearchResults()
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
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } ?: run {
                        // Default to a location if current location is not available
                        val defaultLocation = LatLng(37.7749, -122.4194) // San Francisco
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                    }
                }
        }
    }
    
    private fun searchPlaces(query: String) {
        searchJob?.cancel()
        
        searchJob = searchScope.launch {
            try {
                val token = AutocompleteSessionToken.newInstance()
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setSessionToken(token)
                    .build()
                
                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        val predictions = response.autocompletePredictions
                        if (predictions.isNotEmpty()) {
                            fetchPlaceDetails(predictions)
                        } else {
                            hideSearchResults()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this@MapActivityWithSearch, "Search error: ${exception.message}", Toast.LENGTH_SHORT).show()
                        hideSearchResults()
                    }
            } catch (e: Exception) {
                hideSearchResults()
            }
        }
    }
    
    private fun fetchPlaceDetails(predictions: List<AutocompletePrediction>) {
        val searchResults = mutableListOf<SearchResult>()
        var processedCount = 0
        val maxResults = minOf(5, predictions.size) // Limit to 5 results
        
        predictions.take(maxResults).forEach { prediction ->
            val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
            
            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    place.latLng?.let { latLng ->
                        searchResults.add(
                            SearchResult(
                                placeId = place.id ?: "",
                                name = place.name ?: "Unknown",
                                address = place.address ?: "No address",
                                latLng = latLng
                            )
                        )
                    }
                    
                    processedCount++
                    if (processedCount == maxResults) {
                        showSearchResults(searchResults)
                    }
                }
                .addOnFailureListener {
                    processedCount++
                    if (processedCount == maxResults) {
                        showSearchResults(searchResults)
                    }
                }
        }
    }
    
    private fun showSearchResults(results: List<SearchResult>) {
        if (results.isNotEmpty()) {
            searchAdapter.updateResults(results)
            rvSearchResults.visibility = View.VISIBLE
        } else {
            hideSearchResults()
        }
    }
    
    private fun hideSearchResults() {
        rvSearchResults.visibility = View.GONE
    }
    
    private fun selectSearchResult(searchResult: SearchResult) {
        etSearch.setText(searchResult.name)
        etSearch.clearFocus()
        hideSearchResults()
        
        // Move camera to selected location and add marker
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchResult.latLng, 15f))
        setDestinationMarker(searchResult.latLng)
    }
    
    private fun setDestinationMarker(latLng: LatLng) {
        // Remove existing marker
        destinationMarker?.remove()
        
        // Add new marker
        destinationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Destination")
                .snippet("Selected location")
        )
        
        selectedLocation = latLng
        btnConfirmLocation.visibility = View.VISIBLE
        
        Toast.makeText(this, "Destination selected. Tap confirm to save.", Toast.LENGTH_SHORT).show()
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
    
    override fun onDestroy() {
        super.onDestroy()
        searchScope.cancel()
    }
}
