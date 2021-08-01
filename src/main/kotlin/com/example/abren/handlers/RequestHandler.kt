package com.example.abren.handlers

import com.example.abren.models.Request
import com.example.abren.models.Ride
import com.example.abren.models.User
import com.example.abren.responses.RidesResponse
import com.example.abren.services.RequestService
import com.example.abren.services.RideService
import com.example.abren.services.UserService
import com.mongodb.internal.connection.Server
import org.springframework.http.MediaType
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class RequestHandler(private val requestService: RequestService, private val userService: UserService, private val rideService: RideService) {

    fun createRequest (r: ServerRequest): Mono<ServerResponse>{
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono : Mono<User?> = userService.findByPhoneNumber(securityContext.authentication.principal as String)

            return@flatMap userMono.flatMap second@ { user ->
                val requestMono = r.bodyToMono(Request::class.java)

                return@second requestMono.flatMap { request ->
                    request.riderId = user?._id
                    request.status = "PENDING"
                    val saved  = requestService.create(request)
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                        BodyInserters.fromProducer(saved, Request::class.java)
                    )
                }
            }
        }
    }
}