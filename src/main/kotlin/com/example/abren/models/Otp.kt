package com.example.abren.models

import java.time.LocalDateTime


data class Otp (val code: String, val dateCreated: LocalDateTime, val isValidated: Boolean)
