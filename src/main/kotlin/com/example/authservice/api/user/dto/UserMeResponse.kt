package com.example.authservice.api.user.dto

import java.time.Instant
import java.util.UUID

data class UserMeResponse(
    val id: UUID,
    val email: String,
    val role: String,
    val emailVerified: Boolean,
    val createdAt: Instant
)