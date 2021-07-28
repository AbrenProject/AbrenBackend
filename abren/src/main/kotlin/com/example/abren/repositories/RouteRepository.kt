package com.example.abren.repositories

import com.example.abren.models.Route
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface RouteRepository: ReactiveMongoRepository<Route,Long> {
}