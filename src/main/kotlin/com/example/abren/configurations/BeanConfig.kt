package com.example.abren.configurations

import com.example.abren.handlers.RequestHandler
import com.example.abren.handlers.RideHandler
import com.example.abren.handlers.RouteHandler
import com.example.abren.handlers.UserHandler
import com.example.abren.security.SecurityContextRepository
import com.example.abren.services.RouteService
import com.example.abren.services.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration
class BeanConfig(private val userService: UserService, private val userHandler: UserHandler, private val requestHandler: RequestHandler, private val rideHandler: RideHandler, private val routeService: RouteService, private val routeHandler: RouteHandler) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)

    @Bean
    fun authRoute(): RouterFunction<ServerResponse> {
        return route(POST("/api/auth/login").and(accept(MediaType.APPLICATION_JSON)), userHandler::login)
            .andRoute(POST("/api/auth/signup").and(accept(MediaType.MULTIPART_FORM_DATA)), userHandler::signup)
    }

    @Bean
    fun userRoute(): RouterFunction<ServerResponse> {
        return route(GET("/api/users/profile").and(accept(MediaType.APPLICATION_JSON)), userHandler::getProfile)
//            .andRoute(PUT("/api/users/profile").and(accept(MediaType.APPLICATION_JSON)), userHandler::editUser)
            .andRoute(POST("/api/users/rate/{id}").and(accept(MediaType.APPLICATION_JSON)), userHandler::rate)
    }

    @Bean
    fun requestsRoute(): RouterFunction<ServerResponse> {
        return route(POST("/api/requests").and(accept(MediaType.APPLICATION_JSON)), requestHandler::createRequest)
            .andRoute(PUT("/api/requests/{id}").and(accept(MediaType.APPLICATION_JSON)), requestHandler::sendRequest)

    }

    @Bean
    fun ridesRoute(): RouterFunction<ServerResponse> {
        return route(GET("/api/rides/nearby/{id}").and(accept(MediaType.APPLICATION_JSON)), rideHandler::getRides)
                .andRoute(POST("/api/rides").and(accept(MediaType.APPLICATION_JSON)), rideHandler::createRide)
                .andRoute(GET("/api/rides/requests/{id}").and(accept(MediaType.APPLICATION_JSON)), requestHandler::getRideRequests)
                .andRoute(PUT("/api/rides/{id}").and(accept(MediaType.APPLICATION_JSON)), rideHandler::acceptRequest)
                .andRoute(GET("/api/rides/acceptedrequests/{id}").and(accept(MediaType.APPLICATION_JSON)), rideHandler::getAcceptedRequests)
                .andRoute(GET("/api/rides/allrequests/{id}").and(accept(MediaType.APPLICATION_JSON)), rideHandler::getAllRequests)
                //.andRoute(POST("/api/requests").and(accept(MediaType.MULTIPART_FORM_DATA)), userHandler::signup)

    }

    @Bean
    fun routeRoute():RouterFunction<ServerResponse>{
        return route(GET("/api/routes/{id}").and(accept(MediaType.APPLICATION_JSON)), routeHandler::getRouteById)
                .andRoute(POST("/api/routes").and(accept(MediaType.APPLICATION_JSON)), routeHandler::createRoute)
                .andRoute((GET("/api/routes")).and(accept(MediaType.APPLICATION_JSON)), routeHandler::getRoutes)
                .andRoute(DELETE("/api/routes/{id}").and(accept(MediaType.APPLICATION_JSON)), routeHandler::deleteRoute)
                .andRoute(PUT("/api/routes/{id}").and(accept(MediaType.APPLICATION_JSON)), routeHandler::updateRoute)

    }
}