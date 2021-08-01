package com.example.abren.handlers

import com.example.abren.models.User
import com.example.abren.responses.RidesResponse
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
class RideHandler(private val rideService: RideService, private val userService: UserService, private val requestService: RequestService) {

    var objectToDestinationCluster : Map<String, String> = HashMap()
    var objectToStartCluster : Map<String, String> = HashMap()
    var clusterToObjects: Map<String, Set<String>> = HashMap()

    fun getRides (r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono : Mono<User?> = userService.findByPhoneNumber(securityContext.authentication.principal as String)

            return@flatMap userMono.flatMap second@ { user ->
                val requestMono = requestService.findOne(r.pathVariable("id"))
                requestMono.flatMap third@ { request ->
                    if(user?._id != request?.riderId){
                        return@third ServerResponse.status(401)
                            .body(BodyInserters.fromValue("Request doesn't belong to logged in user."))
                    }else if (request != null){
                        val requested = request.requestedRides
                        val clusterStart = clusterToObjects[objectToStartCluster[request._id]]
                        val clusterDest = clusterToObjects[objectToDestinationCluster[request._id]]
                        if(clusterStart != null && clusterDest != null){
                            val cluster = clusterStart intersect clusterDest
                            return@third ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(RidesResponse(requested, rideService.findAllById(cluster))))
                        }
                    }
                    Mono.empty()
                }
            }
        }
    }

}