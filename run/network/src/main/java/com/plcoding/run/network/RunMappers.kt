package com.plcoding.run.network

import com.plcoding.core.domain.location.Location
import com.plcoding.core.domain.run.Run
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds

//fun RunDto.toRun(): Run {
//    return Run(
//        id = id,
//        duration = durationMillis.milliseconds,
//        dateTimeUtc = Instant.parse(dateTimeUtc)
//            .atZone(ZoneId.of("UTC")),
//        distanceMeters = distanceMeters,
//        location = Location(lat, lon),
//        maxSpeedKmh = maxSpeedKmh,
//        totalElevationMeters = totalElevationMeters,
//        mapPictureUrl = mapPictureUrl
//    )
//}

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration


fun RunDto.toRun(): Run {
    val formatter = DateTimeFormatter.ofPattern("HH-mm-ss dd-MM-yyyy")
    val localDateTime = LocalDateTime.parse(dateTimeUtc, formatter)
    val instant = localDateTime.atZone(ZoneId.of("UTC")).toInstant()

    return Run(
        id = id,
        duration = durationMillis.milliseconds,
        dateTimeUtc = instant.atZone(ZoneId.of("UTC")),
        distanceMeters = distanceMeters,
        location = Location(lat, lon),
        maxSpeedKmh = maxSpeedKmh,
        totalElevationMeters = totalElevationMeters,
        mapPictureUrl = mapPictureUrl,
    )
}

fun Run.toCreateRunRequest(
    mapPictureUrl: String
): CreateRunRequest {
    return CreateRunRequest(
        mapPictureUrl = mapPictureUrl,
        id = id!!,
        durationMillis = duration.inWholeMilliseconds,
        distanceMeters = distanceMeters,
        lat = location.lat,
        lon = location.long,
        avgSpeedKmh = avgSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        totalElevationMeters = totalElevationMeters,
        epochMillis = dateTimeUtc.toEpochSecond() * 1000L
    )
}


fun CreateRunRequest.toRun(): Run {
    return Run(
        id = id.ifBlank { null },
        duration = durationMillis.toDuration(DurationUnit.MILLISECONDS),
        dateTimeUtc = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC),
        distanceMeters = distanceMeters,
        location = Location(lat = lat, long = lon),
        maxSpeedKmh = maxSpeedKmh,
        totalElevationMeters = totalElevationMeters,
        mapPictureUrl = mapPictureUrl
    )
}