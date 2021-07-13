package com.example.abren.configurations

import com.example.abren.handlers.UserHandler
import com.example.abren.services.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration
class BeanConfig(private val userService: UserService, private val userHandler: UserHandler) {

    @Bean
    fun userRoute(): RouterFunction<*>? {
        return route(GET("/users/{id}").and(accept(MediaType.APPLICATION_JSON)), userHandler::getProfile)
    }

    @Bean
    fun authRoute(): RouterFunction<ServerResponse> {
        return route(POST("/auth/login").and(accept(MediaType.APPLICATION_JSON)), userHandler::login)
                .andRoute(POST("/auth/signup").and(accept(MediaType.MULTIPART_FORM_DATA)), userHandler::signup)
    }
}