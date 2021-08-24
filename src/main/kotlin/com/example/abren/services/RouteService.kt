package com.example.abren.services

import com.example.abren.models.Route
import com.example.abren.repositories.RouteRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class RouteService(private val routeRepository: RouteRepository) {
    fun findAll(): Flux<Route>{
        return routeRepository.findAll();
    }

    fun findOne(id:String): Mono<Route>{
        return routeRepository.findById(id)
    }

    fun create(route: Route): Mono<Route>{
        return routeRepository.save(route)
    }

    fun delete(id:String): Mono<Void> {
        return routeRepository.deleteById(id)
    }

    fun update(route:Route): Mono<Route>{
       return routeRepository.save(route)
    }

}