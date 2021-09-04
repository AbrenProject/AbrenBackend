package com.example.abren.models

import com.google.api.gax.longrunning.OperationTimedPollAlgorithm
import java.time.LocalDateTime

data class Ride(
        val _id: String?,
        var driverId: String?,
        var routeId: String?,
        var driverLocation: Location?,
        var status: String?,
        var requests: MutableList<String> = ArrayList(),
        var acceptedRequests: MutableList<String> = ArrayList(),
        var otp:String?, //TODO: Change data type
        var createdAt: LocalDateTime?,
        var updatedAt: LocalDateTime?,
        var deletedAt: LocalDateTime?,
)