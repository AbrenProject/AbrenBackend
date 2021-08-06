package com.example.abren.services

import com.example.abren.models.Request
import com.example.abren.models.Ride
import com.example.abren.repositories.RideRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class RideService(private val rideRepository: RideRepository) {
    fun findAll(): Flux<Ride?> {
        return rideRepository.findAll();
    }

    fun findAllById(ids: Set<String>): Flux<Ride?> {
        return rideRepository.findAllById(ids);
    }

    fun findByStatus(status: String): Flux<Ride?> {
        return rideRepository.findByStatus(status);
    }

    fun findOne(id: String): Mono<Ride?> {
        return rideRepository.findById(id)
    }

    fun update(ride: Ride): Mono<Ride> {
        return rideRepository.save(ride) //TODO: Make better
    }

    fun delete(id: String): Mono<Ride> {
        return rideRepository
            .findById(id)
            .flatMap { u ->
                u?._id?.let { rideRepository.deleteById(it).thenReturn(u) }
            }
    }

    fun count(): Mono<Long> {
        return rideRepository.count()
    }

    fun create(ride: Ride): Mono<Ride> {
        return rideRepository.save(ride)
    }
}