package com.example.abren.responses

import com.example.abren.models.User

data class AuthResponse(
    val user: User,
    val token: String
)
