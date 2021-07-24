package com.example.abren.models

import reactor.core.publisher.Mono

data class AuthResponse(
    val user: User,
    val token: String
)
