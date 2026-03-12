package com.example.authservice.api.auth.dto

import java.util.UUID


data class UserShortResponse(
    val id: UUID,
    val email: String,
    val role: String
)