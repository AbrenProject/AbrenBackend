package com.example.abren.models

import java.time.LocalDateTime
import java.util.*

data class Route(
        var id: String?,
        var driverId: String?,
        var startingLocation: Location,
        var waypointLocations: ArrayList<Location>,
        var destinationLocation: Location,
        var lastTaken: Date?,
        var createdAt: LocalDateTime = LocalDateTime.now(),
        var updatedAt: LocalDateTime = LocalDateTime.now(),
        var deletedAt: LocalDateTime = LocalDateTime.now(),

        )
