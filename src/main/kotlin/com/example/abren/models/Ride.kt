package com.example.abren.models

import java.time.LocalDateTime

data class Ride(
        val _id: String?,
        var driverId: String?,
        var route: Route?,
        var routeId: String?,
        var driverLocation: Location?,
        var status: String?,
        var requests: MutableList<String> = ArrayList(),
        var acceptedRequests: MutableList<String> = ArrayList(),
        var otp:Otp?,
        var createdAt: LocalDateTime? = LocalDateTime.now(),
        var updatedAt: LocalDateTime? = LocalDateTime.now(),
        var deletedAt: LocalDateTime? =null,
)