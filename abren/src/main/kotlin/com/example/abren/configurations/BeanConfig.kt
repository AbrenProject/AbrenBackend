package com.example.abren.configurations

import com.example.abren.handlers.AuthHandler
import com.example.abren.repositories.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse


class BeanConfig(userRepository: UserRepository, authHandler: AuthHandler) {

//    @Bean
//    fun userRoute(): RouterFunction<*>? {
//        val userHandler = UserHandler(userRepository)
//        return RouterFunctions
//            .route(POST("/users").and(accept(APPLICATION_JSON)), userHandler::createUser)
//            .andRoute(GET("/users").and(accept(APPLICATION_JSON)), userHandler::listUser)
//            .andRoute(GET("/users/{userId}").and(accept(APPLICATION_JSON)), userHandler::getUserById)
//            .andRoute(PUT("/users").and(accept(APPLICATION_JSON)), userHandler::createUser)
//            .andRoute(DELETE("/users/userId").and(accept(APPLICATION_JSON)), userHandler::deleteUser)
//    }

    @Bean
    fun authRoute(): RouterFunction<ServerResponse>? {
        return RouterFunctions.route(POST("/auth/login").and(accept(APPLICATION_JSON)), authHandler::login)
            .andRoute(POST("/auth/signup").and(accept(APPLICATION_JSON)), authHandler::signUp)
    }
}