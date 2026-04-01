package com.example.authservice.api.auth.dto

import jakarta.validation.constraints.NotBlank

data class RefreshRequest(
    @field:NotBlank(message = "Refresh token must not be blank")
    val refreshToken: String
)