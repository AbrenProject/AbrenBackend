package com.example.abren.handlers

import com.example.abren.models.Otp
import com.example.abren.models.Request
import com.example.abren.models.Ride
import com.example.abren.models.User
import com.example.abren.responses.BadRequestResponse
import com.example.abren.responses.RidesResponse
import com.example.abren.security.SecurityContextRepository
import com.example.abren.services.RequestService
import com.example.abren.services.RideService
import com.example.abren.services.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.io.*
import java.lang.Boolean.FALSE
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.stream.Collectors


@Component
class RideHandler(
    private val rideService: RideService,
    private val userService: UserService,
    private val requestService: RequestService
) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)

    var startNeighbors: MutableMap<String, MutableSet<String>> = HashMap()
    var destinationNeighbors: MutableMap<String, MutableSet<String>> = HashMap()

    fun createRide(r:ServerRequest): Mono<ServerResponse>{
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> =
                    userService.findByPhoneNumber(securityContext.authentication.principal as String)
            userMono.flatMap { user ->
                val rideMono = r.bodyToMono(Ride::class.java)
                rideMono.flatMap { ride ->
                    ride.driverId = user?._id
                    ride.status="NOT STARTED" //TODO: Check status options
                    ride.createdAt= LocalDateTime.now()
                    val otpCode = ((Math.random() * 900000).toInt() + 100000).toString()
                    val otp = Otp(otpCode,LocalDateTime.now(),FALSE)
                    ride.otp=otp
                    val savedRide = rideService.create(ride)
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                            BodyInserters.fromProducer(savedRide, Request::class.java)
                    )
                }
            }


        }
    }

    fun getRides(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap first@ { securityContext ->
            val userMono: Mono<User?> =
                userService.findByPhoneNumber(securityContext.authentication.principal as String)

            userMono.flatMap second@ { user ->
                val requestMono = requestService.findOne(r.pathVariable("id"))
                requestMono.flatMap third@{ request ->
                    if (user?._id != request?.riderId) {
                        return@third ServerResponse.status(401)
                            .body(BodyInserters.fromValue("Request doesn't belong to logged in user."))
                    } else {
                        val requestedFlux = rideService.findAllById(request?.requestedRides!!)
                        val neighborsStart = startNeighbors[request._id]
                        val neighborsDest = destinationNeighbors[request._id]
                        if (neighborsStart != null && neighborsDest != null) { //TODO: Make sure this handles everything
                            val nearbyIds = neighborsStart intersect neighborsDest
                            logger.info("Cluster: $nearbyIds")
                            return@third rideService.findAllById(nearbyIds).collectList().flatMap fourth@ { nearby -> //TODO: Handle Duplicates
                               requestedFlux.collectList().flatMap { requested ->
                                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                        .body(BodyInserters.fromValue(RidesResponse(requested, nearby)))
                                }
                            }
                        }
                    }
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("There are no nearby rides that match your destination.")))
                }.switchIfEmpty(
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("Request not found.")))
                )
            }
        }
    }
}