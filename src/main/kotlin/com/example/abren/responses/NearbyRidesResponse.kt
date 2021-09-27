package com.example.abren.responses

data class NearbyRidesResponse(
    val startNeighbors: MutableMap<String, MutableSet<String>>,
    val destinationNeighbors: MutableMap<String, MutableSet<String>>
)
