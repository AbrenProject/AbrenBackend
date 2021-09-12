package com.example.abren.requests

import com.example.abren.models.Request
import com.example.abren.models.Ride

data class NearbyRidesRequest(val rides: MutableList<Ride?>?, val requests: MutableList<Request?>?)