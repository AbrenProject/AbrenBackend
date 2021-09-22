package com.example.abren.handlers

import com.example.abren.configurations.Constants
import com.example.abren.models.Location
import com.example.abren.models.Request
import com.example.abren.models.Ride
import com.example.abren.models.User
import com.example.abren.responses.BadRequestResponse
import com.example.abren.responses.RequestsResponse
import com.example.abren.security.SecurityContextRepository
import com.example.abren.services.RequestService
import com.example.abren.services.RideService
import com.example.abren.services.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Component
class RequestHandler(
    private val requestService: RequestService,
    private val userService: UserService,
    private val rideService: RideService
) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)
    val constants = Constants()



    fun createRequest(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> =
                userService.findByPhoneNumber(securityContext.authentication.principal as String)

            userMono.flatMap { user ->
                val requestMono = r.bodyToMono(Request::class.java)

                requestMono.flatMap { request ->
                    request.riderId = user?._id
                    request.status = "PENDING"
                    request.riderGender = user?.gender
                    request.riderAgeGroup = user?.ageGroup
                    request.riderRating = user?.rating
                    val saved = requestService.create(request)
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                        BodyInserters.fromProducer(saved, Request::class.java)
                    )
                }.switchIfEmpty {
                    logger.info("Is Empty")
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required: ${constants.REQUIRED_REQUEST_FIELDS}")))
                }.onErrorResume {
                    logger.info("${it.message}")
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required: ${constants.REQUIRED_REQUEST_FIELDS}")))
                }
            }
        }
    }


    fun getRequests(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap first@{ securityContext ->
            val userMono: Mono<User?> =
                userService.findByPhoneNumber(securityContext.authentication.principal as String)
            val locationMono = r.bodyToMono(Location::class.java)

            userMono.flatMap second@{ user ->
                val rideMono = rideService.findOne(r.pathVariable("id"))
                rideMono.flatMap third@{ ride ->
                    if (user?._id != ride?.driverId) {
                        return@third ServerResponse.status(401)
                            .body(BodyInserters.fromValue(BadRequestResponse("Ride doesn't belong to logged in user.")))
                    } else {
                        return@third locationMono.flatMap fourth@{ location ->
                            ride?.driverLocation = location
                            rideService.update(ride!!).flatMap fifth@{ ride ->
                                val requestedFlux = requestService.findAllById(ride.requests)
                                val acceptedFlux = requestService.findAllById(ride.acceptedRequests)
                                acceptedFlux.collectList()
                                    .flatMap sixth@{ accepted -> //TODO: Handle Duplicates
                                        requestedFlux.collectList().flatMap { requested ->
                                            ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                                .body(BodyInserters.fromValue(RequestsResponse(requested, accepted)))
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
                        .body(BodyInserters.fromValue(BadRequestResponse("Ride not found.")))
                )
            }
        }
    }

    fun sendRequest(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> =
                userService.findByPhoneNumber(securityContext.authentication.principal as String)

            userMono.flatMap { user ->
                val requestMono = requestService.findOne(r.pathVariable("id"))
                requestMono.flatMap third@ { request ->
                    if (user?._id != request?.riderId) {
                        ServerResponse.status(401)
                            .body(BodyInserters.fromValue("Request doesn't belong to logged in user."))
                    } else {
                        r.queryParam("rideId").map { rideId ->
                            val rideMono = rideService.findOne(rideId)
                            rideMono.flatMap { ride ->
                                request?.requestedRides?.add(ride?._id!!)
                                requestService.update(request!!).flatMap {
                                    ride?.requests?.add(request._id!!)
                                    val savedRide = rideService.update(ride!!)

                                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                        .body(BodyInserters.fromProducer(savedRide, Ride::class.java))
                                }

                            }.switchIfEmpty(
                                ServerResponse.badRequest()
                                    .body(BodyInserters.fromValue(BadRequestResponse("Ride not found.")))
                            )
                        }.orElse(
                            ServerResponse.badRequest()
                                .body(BodyInserters.fromValue(BadRequestResponse("The following request parameters are required: [rideId].")))
                        )

                    }
                }.switchIfEmpty(
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("Request not found.")))
                )
            }
        }
    }

    fun startRide(r: ServerRequest): Mono<ServerResponse> {
        val input = r.queryParam("otp")
        val requestMono = requestService.findOne(r.pathVariable("id"))

        return input.map first@ { inputVal ->
            requestMono.flatMap second@ { request ->
                val rideMono = rideService.findOne(request!!.acceptedRide.toString())
                rideMono.flatMap third@ { ride ->
                    if(inputVal != ride?.otp?.code){
                        return@third ServerResponse.badRequest()
                            .body(BodyInserters.fromValue(BadRequestResponse("The code could not be validated.")))
                    }else{
                        request.status = "STARTED"
                        return@third requestService.update(request).flatMap { savedRequest ->
                            ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(savedRequest))
                        }
                    }
                }
            }
        }.orElse(
            ServerResponse.badRequest()
                .body(BodyInserters.fromValue(BadRequestResponse("The following request parameters are required: [otp].")))
        )
    }
}