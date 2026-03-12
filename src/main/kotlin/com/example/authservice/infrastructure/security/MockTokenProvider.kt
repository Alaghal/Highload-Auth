package com.example.authservice.infrastructure.security

import com.example.authservice.infrastructure.persistence.user.UserEntity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MockTokenProvider : TokenProvider {

    override fun generateAccessToken(user: UserEntity): String {
        return "access-${user.id}-${UUID.randomUUID()}"
    }

    override fun generateRefreshToken(user: UserEntity): String {
        return "refresh-${user.id}-${UUID.randomUUID()}"
    }
}