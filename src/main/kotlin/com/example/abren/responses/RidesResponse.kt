package com.example.abren.responses

import com.example.abren.models.Ride

data class RidesResponse(
    var requested: List<Ride?>,
    var nearby: List<Ride?>
)
