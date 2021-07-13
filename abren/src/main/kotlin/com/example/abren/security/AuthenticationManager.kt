package com.example.abren.security

import io.jsonwebtoken.Claims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.stream.Collectors

@Component
class AuthenticationManager(private val tokenProvider: TokenProvider) : ReactiveAuthenticationManager {
    private val constants = Constants()

    @SuppressWarnings("unchecked")
    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val authToken: String = authentication.credentials.toString()
        val phoneNumber: String? = try {
            tokenProvider.getPhoneNumberFromToken(authToken)
        } catch (e: Exception) {
            null
        }
        return if (phoneNumber != null && !tokenProvider.isTokenExpired(authToken)!!) {
            val claims: Claims = tokenProvider.getAllClaimsFromToken(authToken)
            val roles = claims.get(constants.AUTHORITIES_KEY, List::class.java)
            val authorities = roles.stream().map { role ->
                SimpleGrantedAuthority(role as String)
            }.collect(Collectors.toList())
            val auth = UsernamePasswordAuthenticationToken(phoneNumber, phoneNumber, authorities)
            SecurityContextHolder.getContext().authentication = auth
            Mono.just(auth)
        } else {
            Mono.empty()
        }
    }
}