package com.example.abren.configurations

import com.example.abren.security.AuthenticationManager
import com.example.abren.security.SecurityContextRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val authenticationManager: AuthenticationManager,
    private val securityContextRepository: SecurityContextRepository
) {

    @Bean
    fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain? {
        val patterns = arrayOf("/api/auth/**")
        return http.cors().disable()
            .exceptionHandling()
            .authenticationEntryPoint { swe: ServerWebExchange, e: AuthenticationException? ->
                Mono.fromRunnable { swe.response.statusCode = HttpStatus.UNAUTHORIZED }
            }.accessDeniedHandler { swe: ServerWebExchange, e: AccessDeniedException? ->
                Mono.fromRunnable { swe.response.statusCode = HttpStatus.FORBIDDEN }
            }.and()
            .csrf().disable()
            .authenticationManager(authenticationManager)
            .securityContextRepository(securityContextRepository)
            .authorizeExchange()
            .pathMatchers(*patterns).permitAll()
            .pathMatchers(HttpMethod.OPTIONS).permitAll()
//            .pathMatchers("/users/change_account").hasAuthority("RIDER")
            .anyExchange().authenticated()
            .and()
            .build()
    }

}