package com.example.abren.models

import reactor.core.publisher.Flux
import java.time.LocalDateTime

data class Request(
    var _id: String?,
    var riderId: String?,
    var riderGender: String?,
    var riderAgeGroup: String?,
    var riderRating: MutableList<Int>?,
    var riderLocation: Location,
    var destination: Location,
    var status: String?,
    var requestedRides: MutableList<String> = ArrayList(),
    var acceptedRide: String?,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var deletedAt: LocalDateTime? = null,
)
