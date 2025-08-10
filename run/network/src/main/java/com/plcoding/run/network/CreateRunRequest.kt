package com.plcoding.run.network

import kotlinx.serialization.Serializable

@Serializable
data class CreateRunRequest(
    var durationMillis: Long = 0L,
    var distanceMeters: Int = 0,
    var epochMillis: Long = 0L,
    var lat: Double = 0.0,
    var lon: Double = 0.0,
    var avgSpeedKmh: Double = 0.0,
    var maxSpeedKmh: Double = 0.0,
    var totalElevationMeters: Int = 0,
    var id: String = "",
    var mapPictureUrl: String = ""
)