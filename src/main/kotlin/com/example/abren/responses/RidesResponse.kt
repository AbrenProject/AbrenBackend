package com.example.abren.responses

import com.example.abren.models.Ride
import reactor.core.publisher.Flux

data class RidesResponse(
    var requested: Flux<Ride>? = Flux.empty(),
    var nearby: Flux<Ride?>? = Flux.empty()
)
