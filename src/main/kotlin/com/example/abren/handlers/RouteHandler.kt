package com.example.abren.handlers

import com.example.abren.models.Route
import com.example.abren.models.User
import com.example.abren.services.RouteService
import com.example.abren.services.UserService
import org.springframework.http.MediaType
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Component
class RouteHandler (private val routeService: RouteService,private val userService: UserService) {

    fun getRoutes(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> = userService.findByPhoneNumber(securityContext.authentication.principal as String)
            userMono.flatMap { user ->
                val routes = routeService.findAllByDriverId(user?._id!!)
                ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                    BodyInserters.fromProducer(routes, Route::class.java))

            }
        }
    }

    fun getRouteById(r: ServerRequest): Mono<ServerResponse> {
        val routeMono = routeService.findOne(r.pathVariable("id"))
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                BodyInserters.fromProducer(routeMono, Route::class.java))

    }

    fun deleteRoute(r: ServerRequest): Mono<ServerResponse> {
        val deletedRoute = routeService.delete(r.pathVariable("id"))
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(deletedRoute, Void::class.java)
    }

    fun updateRoute(r: ServerRequest): Mono<ServerResponse> {
        val toBeUpdatedMono: Mono<Route?> = routeService.findOne(r.pathVariable("id"))

        return toBeUpdatedMono.flatMap { toBeUpdated ->
            val routeUpdateRequestMono = r.bodyToMono(Route::class.java)

            routeUpdateRequestMono.flatMap { routeUpdateRequest ->

                toBeUpdated?.startingLocation = routeUpdateRequest.startingLocation
                toBeUpdated?.waypointLocations = routeUpdateRequest.waypointLocations
                toBeUpdated?.destinationLocation = routeUpdateRequest.destinationLocation

                toBeUpdated?.updatedAt = LocalDateTime.now()

                var updatedRouteMono: Mono<Route> = Mono.empty()
                if (toBeUpdated != null) {
                    updatedRouteMono = routeService.update(toBeUpdated)
                }
                updatedRouteMono.flatMap { updatedRoute ->
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                            BodyInserters.fromValue(updatedRoute))
                }
            }
        }
    }

    fun createRoute(r: ServerRequest): Mono<ServerResponse> {
        val routeMono = r.bodyToMono(Route::class.java)
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> = userService.findByPhoneNumber(securityContext.authentication.principal as String)
            userMono.flatMap { user ->
                routeMono.flatMap { route ->
                    route.driverId = user?._id
                    route.createdAt = LocalDateTime.now()
                    val savedRoute = routeService.create(route)

                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                            BodyInserters.fromProducer(savedRoute, Route::class.java))

                }
            }
        }
    }
}