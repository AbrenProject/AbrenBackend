package com.example.abren.models

import java.time.LocalDate

data class User(
    var _id: String?,
    var name: Name?,
    var phoneNumber: String,
    var gender: String?,
    var ageGroup: String?,
    var password: String,
    var role: String?,
    var isVerified: Boolean?,
    var emergencyPhoneNumber: String,
    var profilePictureUrl: String?,
    var idCardUrl: String?,
    var vehicleInformation: VehicleInformation?,
    var preference: List<Preference>?,
    var rating: Double = 0.0,
    var creditsBought: Double = 0.0,
    var creditsEarned: Double = 0.0,
    var createdAt: LocalDate = LocalDate.now(),
    var updatedAt: LocalDate = LocalDate.now(),
    var deletedAt: LocalDate = LocalDate.now(),
)
