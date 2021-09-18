package com.example.abren.services

import com.example.abren.models.Request
import com.example.abren.models.Ride
import com.example.abren.repositories.RideRepository
import com.example.abren.requests.DocumentVerifierRequest
import com.example.abren.requests.NearbyRidesRequest
import com.example.abren.responses.DocumentVerifierResponse
import com.example.abren.responses.NearbyRidesResponse
import com.example.abren.security.SecurityContextRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class RideService(private val rideRepository: RideRepository, private val requestService: RequestService, private val webClientBuilder: WebClient.Builder) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)
    fun findAll(): Flux<Ride?> {
        return rideRepository.findAll();
    }

    fun findAllById(ids: Set<String>): Flux<Ride?> {
        return rideRepository.findAllById(ids);
    }

    fun findAllById(ids: MutableList<String>): Flux<Ride?> {
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

    fun getNearby(rides: MutableList<Ride?>?, requests: MutableList<Request?>?): Mono<NearbyRidesResponse> {
        val webClient = webClientBuilder.baseUrl("http://localhost:5000/").build()

        val nearbyRidesRequest = NearbyRidesRequest(rides, requests)
        return webClient.post()
            .uri("/nearest-rides")
            .body(Mono.just(nearbyRidesRequest), NearbyRidesRequest::class.java)
            .retrieve()
            .bodyToMono(NearbyRidesResponse::class.java)
    }
}