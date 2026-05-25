package com.aura.records

import kotlinx.serialization.Serializable

@Serializable
data class Geolocation(
    val latitude: Double,
    val longitude: Double,
    val geohash: String? = null
)