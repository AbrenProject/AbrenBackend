package com.example.abren.models

import java.time.LocalDateTime

data class Ride(
    val _id: String?,
    var driverId: String?,
    var route: Route?,
    var routeId: String?,
    var driverGender: String?,
    var driverAgeGroup: String?,
    var driverRating: MutableList<Int>?,
    var driverLocation: Location?,
    var status: String?,
    var requests: MutableList<String> = ArrayList(),
    var acceptedRequests: MutableList<String> = ArrayList(),
    var otp: Otp?, //TODO: Make sure this is not sent to riders
    var cost: Double = 0.0,
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var deletedAt: LocalDateTime? = null,
)