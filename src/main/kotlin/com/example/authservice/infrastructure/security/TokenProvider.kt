package com.example.authservice.infrastructure.security

import com.example.authservice.infrastructure.persistence.user.UserEntity

interface TokenProvider {
    fun generateAccessToken(user: UserEntity): String
    fun generateRefreshToken(user: UserEntity): String
}