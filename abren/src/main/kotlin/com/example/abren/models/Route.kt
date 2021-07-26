package com.example.abren.models

import java.util.*

data class Route(
        var id: Long,
        var driverId: Long,
        var startingLocation: Location,
        var waypointLocations: ArrayList<Location>,
        var destinationLocation: Location,
        var lastTaken: Date,
        var createdAt: Date,
        var updatedAt: Date,
        var deletedAt: Date,

        )
