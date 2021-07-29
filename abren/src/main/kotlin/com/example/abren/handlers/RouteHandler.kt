package com.example.abren.handlers

import com.example.abren.models.Route
import com.example.abren.services.RouteService
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import javax.print.attribute.standard.Media

@Component
class RouteHandler (private val routeService: RouteService){
//fun getRoutes(r: ServerRequest): Mono<ServerResponse>
fun getRouteById(r: ServerRequest): Mono<ServerResponse>{
    val routeMono = routeService.findOne(r.pathVariable("id"))
    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
            BodyInserters.fromProducer(routeMono,Route::class.java))

}

fun createRoute(r: ServerRequest): Mono<ServerResponse> {
    val routeMono = r.bodyToMono(Route::class.java)
    return routeMono.flatMap { route->
        routeService.create(route).flatMap {
            savedRoute -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                BodyInserters.fromValue(savedRoute)
            )
        }
    }
}
//fun editRoute(r: ServerRequest): Mono<ServerResponse>
//fun deleteRoute(r: ServerRequest): Mono<ServerResponse>
}