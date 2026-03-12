package com.example.authservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpirationMinutes: Long,
    val refreshTokenExpirationDays: Long
)