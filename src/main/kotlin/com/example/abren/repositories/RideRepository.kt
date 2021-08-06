package com.example.abren.repositories

import com.example.abren.models.Ride
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface RideRepository : ReactiveMongoRepository<Ride?, String?> {
    fun findByStatus(status: String): Flux<Ride?>
}