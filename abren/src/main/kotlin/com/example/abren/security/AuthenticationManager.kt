package com.example.abren.security

import io.jsonwebtoken.Claims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Mono
import java.util.stream.Collectors


class AuthenticationManager(private val tokenProvider: TokenProvider) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<*>? {
        val authToken: String = authentication.getCredentials().toString()
        val username: String?
        username = try {
            tokenProvider.getUsernameFromToken(authToken)
        } catch (e: Exception) {
            null
        }
        return if (username != null && !tokenProvider.isTokenExpired(authToken)) {
            val claims: Claims = tokenProvider.getAllClaimsFromToken(authToken)
            val roles = claims.get<List<*>>(AUTHORITIES_KEY, MutableList::class.java)
            val authorities = roles.stream().map { role: Any? ->
                SimpleGrantedAuthority(
                    role
                )
            }.collect(Collectors.toList<Any>())
            val auth = UsernamePasswordAuthenticationToken(username, username, authorities)
            SecurityContextHolder.getContext().authentication = AuthenticatedUser(username, authorities)
            Mono.just(auth)
        } else {
            Mono.empty<Any>()
        }
    }
}