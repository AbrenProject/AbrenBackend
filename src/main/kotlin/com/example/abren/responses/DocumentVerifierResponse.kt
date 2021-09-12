package com.example.abren.responses

data class DocumentVerifierResponse(val data: DocumentData, val isVerified: Boolean, val isTextVerified: Boolean, val isFaceVerified: Boolean, val isLogoVerified: Boolean)

data class DocumentData(
    val dateOfBirth: String? = null,
    val expiryDate: String,
    val isValid: Boolean,
    val issueDate: String,
    val name: String? = null,
    val sex: String? = null
)
