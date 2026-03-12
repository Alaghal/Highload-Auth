package com.example.authservice.api.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(

    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email must not be blank")
    val email: String,

    @field:NotBlank(message = "Password must not be blank")
    val password: String
)