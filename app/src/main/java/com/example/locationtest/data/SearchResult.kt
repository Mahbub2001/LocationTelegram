package com.example.locationtest.data

import com.google.android.gms.maps.model.LatLng

data class SearchResult(
    val placeId: String,
    val name: String,
    val address: String,
    val latLng: LatLng
)
