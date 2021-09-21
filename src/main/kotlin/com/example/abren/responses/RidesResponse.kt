package com.example.abren.responses

import com.example.abren.models.Ride
import com.example.abren.models.User

data class RidesResponse(
    var requested: List<Ride?>,
    var nearby: List<Ride?>,
    var accepted: Ride?
)
