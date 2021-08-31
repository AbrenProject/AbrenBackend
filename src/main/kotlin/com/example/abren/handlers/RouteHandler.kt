package com.example.abren.handlers

import com.example.abren.models.Location
import com.example.abren.models.Route
import com.example.abren.services.RouteService
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractor
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import javax.print.attribute.standard.Media

@Component
class RouteHandler (private val routeService: RouteService) {

    fun getRoutes(r: ServerRequest): Mono<ServerResponse> {
        val routesFlux = routeService.findAll()
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        BodyInserters.fromProducer(routesFlux, Route::class.java))

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
        val toBeUpdatedMono : Mono<Route?> = routeService.findOne(r.pathVariable("id"))

        return toBeUpdatedMono.flatMap { toBeUpdated->
            val routeUpdateRequestMono = r.bodyToMono(Route::class.java)

            routeUpdateRequestMono.flatMap { routeUpdateRequest ->

                toBeUpdated?.startingLocation=routeUpdateRequest.startingLocation
                toBeUpdated?.waypointLocations=routeUpdateRequest.waypointLocations
                toBeUpdated?.destinationLocation=routeUpdateRequest.destinationLocation

                toBeUpdated?.updatedAt= LocalDateTime.now()

                var updatedRouteMono : Mono<Route> = Mono.empty()
                if(toBeUpdated!= null){
                    updatedRouteMono = routeService.update(toBeUpdated)
                }
                updatedRouteMono.flatMap { updatedRoute->
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                            BodyInserters.fromValue(updatedRoute))
                }
            }
        }
    }

fun createRoute(r: ServerRequest): Mono<ServerResponse> {
    val routeMono = r.bodyToMono(Route::class.java)
    return routeMono.flatMap { route->
        route.driverId = "id1"
        route.createdAt = LocalDateTime.now()
        val savedRoute = routeService.create(route)
        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                BodyInserters.fromProducer(savedRoute,Route::class.java))

    }
}
}