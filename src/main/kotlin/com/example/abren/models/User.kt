package com.example.abren.models

import java.time.LocalDateTime

data class User(
    var _id: String? = null,
    var name: Name? = null,
    var phoneNumber: String,
    var gender: String? = null,
    var ageGroup: String? = null,
    var password: String,
    var role: String, //TODO: Create enum
    var isVerified: Boolean = false,
    var emergencyPhoneNumber: String,
    var profilePictureUrl: String,
    var idCardUrl: String,
    var idCardBackUrl: String,
    var vehicleInformation: VehicleInformation? = null,
    var preference: List<Preference>? = null,
    var rating: MutableList<Int> = MutableList(5) { 0 },
    var creditsBought: Double = 0.0,
    var creditsEarned: Double = 0.0,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var deletedAt: LocalDateTime? = null,
)