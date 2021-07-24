package com.example.abren.security

import com.example.abren.configurations.Constants
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class SecurityContextRepository(private val authenticationManager: AuthenticationManager) : ServerSecurityContextRepository {
    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)
    private val constants = Constants()

    override fun save(swe: ServerWebExchange?, sc: SecurityContext?): Mono<Void>? {
        throw UnsupportedOperationException("Not supported yet.")
    }

    override fun load(swe: ServerWebExchange): Mono<SecurityContext>? {
        val request: ServerHttpRequest = swe.request
        val authHeader: String? = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        var authToken: String? = null
        if (authHeader != null && authHeader.startsWith(constants.TOKEN_PREFIX)) {
            authToken = authHeader.replace(constants.TOKEN_PREFIX, "")
        } else {
            logger.warn("Couldn't find bearer string, will ignore the header.")
        }
        return if (authToken != null) {
            val auth: Authentication = UsernamePasswordAuthenticationToken(authToken, authToken)
            authenticationManager.authenticate(auth).map { authentication: Any ->
                SecurityContextImpl(authentication as Authentication)
            }
        } else {
            Mono.empty()
        }
    }
}