package com.example.abren.handlers

import com.example.abren.configurations.Constants
import com.example.abren.models.Request
import com.example.abren.models.Ride
import com.example.abren.models.User
import com.example.abren.responses.AuthResponse
import com.example.abren.responses.RidesResponse
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
import org.springframework.web.reactive.function.server.EntityResponse.fromObject
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

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
                    val saved = requestService.create(request)
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                        BodyInserters.fromProducer(saved, Request::class.java)
                    )
                }.switchIfEmpty(
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue("The following fields are required: ${constants.REQUIRED_REQUEST_FIELDS}"))
                )
            }
        }
    }

    fun getRideRequests(r:ServerRequest): Mono<ServerResponse>{
        val rideMono = rideService.findOne(r.pathVariable("id"))
       return rideMono.flatMap { ride ->
            val rideRequestsId = ride?.requests!!
            val rideRequests = requestService.findAllByIds(rideRequestsId)
            ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromProducer(rideRequests, Request::class.java))
        }.switchIfEmpty(
               ServerResponse.badRequest()
                       .body(BodyInserters.fromValue("Ride not found."))
       )


    }

    fun getAllRequests(r:ServerRequest): Mono<ServerResponse>{
        val requestsFlux = requestService.findAll()
         return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromProducer(requestsFlux, Request::class.java))

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
                                    .body(BodyInserters.fromValue("Ride not found."))
                            )
                        }.orElse(
                            ServerResponse.badRequest()
                                .body(BodyInserters.fromValue("The following request parameters are required: [rideId]."))
                        )

                    }
                }.switchIfEmpty(
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue("Request not found."))




                )
            }
        }
    }
}