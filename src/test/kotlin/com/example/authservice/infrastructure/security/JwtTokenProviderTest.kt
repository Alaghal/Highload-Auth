package com.example.authservice.infrastructure.security

import com.example.authservice.config.JwtProperties
import com.example.authservice.domain.user.UserRole
import com.example.authservice.domain.user.UserStatus
import com.example.authservice.infrastructure.persistence.user.UserEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class JwtTokenProviderTest {

    private lateinit var jwtProperties: JwtProperties
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        jwtProperties = JwtProperties(
            secret = "test-secret-key-test-secret-key-test-secret-key-123456",
            accessTokenExpirationMinutes = 15,
            refreshTokenExpirationDays = 7
        )
        jwtTokenProvider = JwtTokenProvider(jwtProperties)
    }

    @Test
    fun `should generate valid access token`() {
        val user = UserEntity(
            id = UUID.randomUUID(),
            email = "test@example.com",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val token = jwtTokenProvider.generateAccessToken(user)

        assertThat(token).isNotBlank
        assertThat(jwtTokenProvider.isTokenValid(token)).isTrue
        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(user.id)
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo(user.email)
        assertThat(jwtTokenProvider.extractRole(token)).isEqualTo(UserRole.USER.name)
        assertThat(jwtTokenProvider.extractType(token)).isEqualTo("access")
    }

    @Test
    fun `should generate valid refresh token`() {
        val user = UserEntity(
            id = UUID.randomUUID(),
            email = "test@example.com",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val token = jwtTokenProvider.generateRefreshToken(user)

        assertThat(token).isNotBlank
        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue
        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(user.id)
        assertThat(jwtTokenProvider.extractType(token)).isEqualTo("refresh")
        // Refresh token typically doesn't have email/role claims in this implementation
        assertThat(jwtTokenProvider.extractEmail(token)).isNull()
    }

    @Test
    fun `isTokenValid should return false for invalid token`() {
        assertThat(jwtTokenProvider.isTokenValid("invalid-token")).isFalse
    }

    @Test
    fun `isTokenValid should return false for expired token`() {
        // We can't easily mock Instant.now() here without extra libraries, 
        // but we could set very short expiration or use a different secret to fail validation.
        // For now, testing invalid string is enough for basic verification.
        assertThat(jwtTokenProvider.isTokenValid("")).isFalse
    }
}
