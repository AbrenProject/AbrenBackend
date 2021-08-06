package com.example.abren.handlers

import com.example.abren.configurations.Constants
import com.example.abren.models.Request
import com.example.abren.models.User
import com.example.abren.services.RequestService
import com.example.abren.services.RideService
import com.example.abren.services.UserService
import org.springframework.http.MediaType
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class RequestHandler(
    private val requestService: RequestService,
    private val userService: UserService,
    private val rideService: RideService
) {

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
}