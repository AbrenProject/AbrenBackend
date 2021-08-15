package com.example.abren.handlers

import com.example.abren.models.Route
import com.example.abren.services.RouteService
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import javax.print.attribute.standard.Media

@Component
class RouteHandler (private val routeService: RouteService){

fun getRouteById(r: ServerRequest): Mono<ServerResponse>{
    val routeMono = routeService.findOne(r.pathVariable("id"))
    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
            BodyInserters.fromProducer(routeMono,Route::class.java))

}

fun createRoute(r: ServerRequest): Mono<ServerResponse> {
    val routeMono = r.bodyToMono(Route::class.java)
    return routeMono.flatMap { route->
        route.driverId = "id1"
        val savedRoute = routeService.create(route)
        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                BodyInserters.fromProducer(savedRoute,Route::class.java))

    }
}

}