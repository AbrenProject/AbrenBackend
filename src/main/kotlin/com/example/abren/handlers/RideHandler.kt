package com.example.abren.handlers

import com.example.abren.models.*
import com.example.abren.responses.BadRequestResponse
import com.example.abren.responses.NearbyRidesResponse
import com.example.abren.responses.RequestsResponse
import com.example.abren.responses.RidesResponse
import com.example.abren.security.SecurityContextRepository
import com.example.abren.services.RequestService
import com.example.abren.services.RideService
import com.example.abren.services.RouteService
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.io.*
import java.lang.Boolean.FALSE
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap


@Component
class RideHandler(
    private val rideService: RideService,
    private val routeService: RouteService,
    private val userService: UserService,
    private val requestService: RequestService
) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)

    var startNeighbors: MutableMap<String, MutableSet<String>> = HashMap()
    var destinationNeighbors: MutableMap<String, MutableSet<String>> = HashMap()

    //TODO: UPDATE THE STATUS OF REQUEST TO ACCEPTED
    fun acceptRequest(r: ServerRequest): Mono<ServerResponse> {
        val rideMono = rideService.findOne(r.pathVariable("id"))
        return rideMono.flatMap { ride ->
            r.queryParam("requestId").map { requestId ->
                val requestMono = requestService.findOne(requestId)
                requestMono.flatMap { request ->
                    request?.acceptedRide = r.pathVariable("id")
                    request?.status = "ACCEPTED"
                    val savedRequestMono = requestService.update(request!!)
                    savedRequestMono.flatMap {
                        ride?.acceptedRequests?.add(requestId)
                        ride?.requests?.remove(requestId)
                        val updatedRide = rideService.update(ride!!)
                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                            BodyInserters.fromProducer(updatedRide, Ride::class.java)
                        )
                    }
                }.switchIfEmpty(
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("Request not found.")))
                )

            }.orElse(
                ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("The following request parameters are required: [requestId].")))
            )
        }.switchIfEmpty(
            ServerResponse.badRequest()
                .body(BodyInserters.fromValue(BadRequestResponse("Ride not found.")))
        )
    }

    fun createRide(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> =
                userService.findByPhoneNumber(securityContext.authentication.principal as String)
            userMono.flatMap { user ->
                val rideMono = r.bodyToMono(Ride::class.java)
                rideMono.flatMap { ride ->
                    val routeMono = routeService.findOne(ride.routeId!!)
                    routeMono.flatMap { route ->
                        ride.driverId = user?._id
                        ride.status = "ACTIVE"
                        ride.route = route
                        val otpCode = ((Math.random() * 900000).toInt() + 100000).toString()
                        val otp = Otp(otpCode, LocalDateTime.now(), FALSE)
                        ride.otp = otp
                        val savedRide = rideService.create(ride)
                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                            BodyInserters.fromProducer(savedRide, Request::class.java)
                        )
                    }.switchIfEmpty(
                        ServerResponse.badRequest()
                            .body(BodyInserters.fromValue(BadRequestResponse("Route not found.")))
                    )
                }
            }
        }
    }

    fun getRides(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap first@{ securityContext ->
            val userMono: Mono<User?> =
                userService.findByPhoneNumber(securityContext.authentication.principal as String)
            val locationMono = r.bodyToMono(Location::class.java)

            userMono.flatMap second@{ user ->
                val requestMono = requestService.findOne(r.pathVariable("id"))
                requestMono.flatMap third@{ request ->
                    if (user?._id != request?.riderId) {
                        return@third ServerResponse.status(401)
                            .body(BodyInserters.fromValue(BadRequestResponse("Request doesn't belong to logged in user.")))
                    } else {
                        return@third locationMono.flatMap fourth@{ location ->
                            request?.riderLocation = location
                            requestService.update(request!!).flatMap fifth@{ request ->
                                val requestedFlux = rideService.findAllById(request?.requestedRides!!)
                                val neighborsStart = startNeighbors[request._id]
                                val neighborsDest = destinationNeighbors[request._id]
                                if (neighborsStart != null && neighborsDest != null) { //TODO: Make sure this handles everything
                                    val nearbyIds = neighborsStart intersect neighborsDest
                                    logger.info("Cluster: $nearbyIds")
                                    return@fifth rideService.findAllById(nearbyIds).collectList()
                                        .flatMap sixth@{ nearby -> //TODO: Handle Duplicates
                                            requestedFlux.collectList().flatMap { requested ->
                                                if(request.acceptedRide != null){
                                                    rideService.findOne(request.acceptedRide!!).flatMap { acceptedRide ->
                                                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                                            .body(BodyInserters.fromValue(RidesResponse(requested, nearby, acceptedRide)))
                                                    }
                                                }else {
                                                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                                        .body(BodyInserters.fromValue(RidesResponse(requested, nearby, null)))
                                                }

                                            }
                                        }
                                } else {
                                    rideService.findOne(request.acceptedRide!!).flatMap { acceptedRide ->
                                        if(acceptedRide?.status == "FINISHED" && request.status == "STARTED"){
                                            request.status = "FINISHED"
                                            userService.findOne(request.riderId!!).flatMap { user ->
                                                user?.creditsBought = user!!.creditsBought.minus(acceptedRide.cost)
                                                userService.update(user).flatMap { savedUser ->
                                                    requestService.update(request).flatMap { savedRequest ->
                                                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                                            .body(BodyInserters.fromValue(RidesResponse(emptyList(), emptyList(), acceptedRide)))
                                                    }
                                                }
                                            }
                                        }else{
                                            ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                                .body(BodyInserters.fromValue(RidesResponse(emptyList(), emptyList(), acceptedRide)))
                                        }
                                    }
                                }
                            }
                        }.switchIfEmpty(
                            ServerResponse.badRequest()
                                .body(BodyInserters.fromValue(BadRequestResponse("Invalid Input for Location")))
                        )
                    }
                }.switchIfEmpty(
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("Request not found.")))
                )
            }
        }
    }

    fun finishRide(r: ServerRequest): Mono<ServerResponse> {
        val input = r.queryParam("km")
        val rideMono = rideService.findOne(r.pathVariable("id"))

        return input.map first@ { inputVal ->
            rideMono.flatMap <ServerResponse> second@ { ride ->
                val userMono = userService.findOne(ride?.driverId.toString())
                val requestsFlux = requestService.findAllById(ride!!.acceptedRequests )
                requestsFlux.collectList().flatMap { requests ->
                    userMono.flatMap { user ->
                        val fuel = inputVal.toDouble() / user?.vehicleInformation!!.kml
                        val cost = fuel * 21.5
                        user.creditsEarned = user.creditsEarned.plus(cost)
                        userService.update(user).flatMap { savedUser ->
                            ride.cost = cost / ride.acceptedRequests.size
                            ride.status = "FINISHED"
                            rideService.update(ride).flatMap { savedRide ->
                                ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromValue(savedRide))
                            }
                        }
                    }
                }
            }.switchIfEmpty(
                ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("Ride not Found.")))
            ).onErrorResume {
                ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("Ride not Found.")))
            }
        }.orElse(
            ServerResponse.badRequest()
                .body(BodyInserters.fromValue(BadRequestResponse("The following request parameters are required: [km].")))
        )
    }

    @Scheduled(fixedDelay = 20000)
    fun prepareRides() { //TODO: Run in thread?
        logger.info("Preparing Rides")
        val activeRides = rideService.findByStatus("ACTIVE")
        val activeRequests = requestService.findByStatus("PENDING")

        val requestsList = activeRequests.collectList().block()
        val ridesList = activeRides.collectList().block()

        val nearby = rideService.getNearby(ridesList, requestsList).block()
        if (nearby != null) {
            startNeighbors = nearby.startNeighbors
        }
        if (nearby != null) {
            destinationNeighbors = nearby.destinationNeighbors
        }
    }
}