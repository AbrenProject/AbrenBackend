package com.example.abren.repositories

import com.example.abren.models.Route
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface RouteRepository: ReactiveMongoRepository<Route,String> {
    fun findAllByDriverId(driverId: String): Flux<Route?>
}