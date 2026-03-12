package com.example.authservice.api.auth.dto

data class AuthResponse(
    val user: UserShortResponse,
    val accessToken: String,
    val refreshToken: String
)