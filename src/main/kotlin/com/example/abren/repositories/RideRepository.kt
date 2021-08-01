package com.example.abren.repositories

import com.example.abren.models.Ride
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface RideRepository : ReactiveMongoRepository<Ride?, String?> {
}