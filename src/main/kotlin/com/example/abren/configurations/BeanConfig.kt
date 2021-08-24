package com.example.abren.configurations

import com.example.abren.handlers.RouteHandler
import com.example.abren.handlers.UserHandler
import com.example.abren.services.RouteService
import com.example.abren.services.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration
class BeanConfig(private val userService: UserService, private val userHandler: UserHandler, private val routeService: RouteService, private val routeHandler: RouteHandler) {

    @Bean
    fun userRoute(): RouterFunction<ServerResponse> {
        return route(GET("/api/users/profile").and(accept(MediaType.APPLICATION_JSON)), userHandler::getProfile)
                .andRoute(PUT("/api/users/profile").and(accept(MediaType.APPLICATION_JSON)), userHandler::editUser)
                .andRoute(POST("/api/users/rate/{id}").and(accept(MediaType.APPLICATION_JSON)), userHandler::rate)

    }

    @Bean
    fun authRoute(): RouterFunction<ServerResponse> {
        return route(POST("/api/auth/login").and(accept(MediaType.APPLICATION_JSON)), userHandler::login)
                .andRoute(POST("/api/auth/signup").and(accept(MediaType.MULTIPART_FORM_DATA)), userHandler::signup)
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