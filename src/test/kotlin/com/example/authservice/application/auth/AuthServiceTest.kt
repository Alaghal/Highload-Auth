package com.example.authservice.application.auth

import com.example.authservice.api.auth.dto.LoginRequest
import com.example.authservice.api.auth.dto.RegisterRequest
import com.example.authservice.common.exception.ConflictException
import com.example.authservice.common.exception.UnauthorizedException
import com.example.authservice.domain.user.UserRole
import com.example.authservice.domain.user.UserStatus
import com.example.authservice.infrastructure.persistence.token.RefreshTokenEntity
import com.example.authservice.infrastructure.persistence.token.RefreshTokenRepository
import com.example.authservice.infrastructure.persistence.user.UserEntity
import com.example.authservice.infrastructure.persistence.user.UserRepository
import com.example.authservice.infrastructure.security.JwtTokenProvider
import com.example.authservice.infrastructure.security.TokenHashService
import com.example.authservice.infrastructure.security.TokenProvider
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.*

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var tokenProvider: TokenProvider
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var tokenHashService: TokenHashService
    private lateinit var authService: AuthService
    private lateinit var httpRequest: HttpServletRequest

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        tokenProvider = mock(TokenProvider::class.java)
        jwtTokenProvider = mock(JwtTokenProvider::class.java)
        tokenHashService = mock(TokenHashService::class.java)
        httpRequest = mock(HttpServletRequest::class.java)

        authService = AuthService(
            userRepository,
            refreshTokenRepository,
            passwordEncoder,
            tokenProvider,
            jwtTokenProvider,
            tokenHashService
        )
    }

    @Test
    fun `register should throw ConflictException if user already exists`() {
        val request = RegisterRequest("exists@example.com", "Password123")
        `when`(userRepository.existsByEmail("exists@example.com")).thenReturn(true)

        assertThrows<ConflictException> {
            authService.register(request, httpRequest)
        }
    }

    @Test
    fun `login should throw UnauthorizedException for invalid credentials`() {
        val request = LoginRequest("user@example.com", "wrong-pass")
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(null)

        assertThrows<UnauthorizedException> {
            authService.login(request, httpRequest)
        }
    }

    @Test
    fun `login should throw UnauthorizedException for incorrect password`() {
        val email = "user@example.com"
        val password = "correct-pass"
        val request = LoginRequest(email, "wrong-pass")
        val user = UserEntity(
            id = UUID.randomUUID(),
            email = email,
            passwordHash = "hashed-password",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        `when`(userRepository.findByEmail(email)).thenReturn(user)
        `when`(passwordEncoder.matches("wrong-pass", "hashed-password")).thenReturn(false)

        assertThrows<UnauthorizedException> {
            authService.login(request, httpRequest)
        }
    }

    @Test
    fun `login should succeed with correct credentials`() {
        val email = "user@example.com"
        val password = "password123"
        val request = LoginRequest(email, password)
        val user = UserEntity(
            id = UUID.randomUUID(),
            email = email,
            passwordHash = "hashed-password",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        `when`(userRepository.findByEmail(email)).thenReturn(user)
        `when`(passwordEncoder.matches(password, "hashed-password")).thenReturn(true)
        `when`(tokenProvider.generateAccessToken(user)).thenReturn("access-token")
        `when`(tokenProvider.generateRefreshToken(user)).thenReturn("refresh-token")
        `when`(tokenHashService.sha256("refresh-token")).thenReturn("hashed-refresh-token")
        `when`(jwtTokenProvider.extractExpiration(anyString())).thenReturn(Instant.now().plusSeconds(3600))

        val result = authService.login(request, httpRequest)

        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertThat(result.user.email).isEqualTo(email)
        verify(refreshTokenRepository).save(any())
    }

    @Test
    fun `register should succeed with valid data`() {
        val email = "new@example.com"
        val password = "StrongPassword123"
        val request = RegisterRequest(email, password)
        val savedUser = UserEntity(
            id = UUID.randomUUID(),
            email = email,
            passwordHash = "hashed-password",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        `when`(userRepository.existsByEmail(email)).thenReturn(false)
        `when`(passwordEncoder.encode(password)).thenReturn("hashed-password")
        `when`(userRepository.save(any())).thenReturn(savedUser)
        `when`(tokenProvider.generateAccessToken(savedUser)).thenReturn("access-token")
        `when`(tokenProvider.generateRefreshToken(savedUser)).thenReturn("refresh-token")
        `when`(tokenHashService.sha256("refresh-token")).thenReturn("hashed-refresh-token")
        `when`(jwtTokenProvider.extractExpiration(anyString())).thenReturn(Instant.now().plusSeconds(3600))

        val result = authService.register(request, httpRequest)

        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertThat(result.user.email).isEqualTo(email)
        verify(userRepository).save(any())
        verify(refreshTokenRepository).save(any())
    }

    @Test
    fun `refresh should rotate tokens`() {
        val oldRefreshToken = "old-refresh-token"
        val oldTokenHash = "old-token-hash"
        val userId = UUID.randomUUID()
        val user = UserEntity(
            id = userId,
            email = "user@example.com",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val storedToken = RefreshTokenEntity(
            id = UUID.randomUUID(),
            userId = userId,
            tokenHash = oldTokenHash,
            expiresAt = Instant.now().plusSeconds(3600),
            revoked = false,
            createdAt = Instant.now()
        )

        `when`(jwtTokenProvider.isRefreshToken(oldRefreshToken)).thenReturn(true)
        `when`(tokenHashService.sha256(oldRefreshToken)).thenReturn(oldTokenHash)
        `when`(refreshTokenRepository.findByTokenHash(oldTokenHash)).thenReturn(storedToken)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(tokenProvider.generateAccessToken(user)).thenReturn("new-access-token")
        `when`(tokenProvider.generateRefreshToken(user)).thenReturn("new-refresh-token")
        `when`(tokenHashService.sha256("new-refresh-token")).thenReturn("new-token-hash")
        `when`(jwtTokenProvider.extractExpiration("new-refresh-token")).thenReturn(Instant.now().plusSeconds(3600))

        val result = authService.refresh(com.example.authservice.api.auth.dto.RefreshRequest(oldRefreshToken), httpRequest)

        assertThat(result.accessToken).isEqualTo("new-access-token")
        assertThat(result.refreshToken).isEqualTo("new-refresh-token")
        assertThat(storedToken.revoked).isTrue()
        verify(refreshTokenRepository).save(storedToken)
        verify(refreshTokenRepository).save(argThat { it.tokenHash == "new-token-hash" })
    }

    @Test
    fun `logout should revoke token if found`() {
        val refreshToken = "some-refresh-token"
        val tokenHash = "hashed-token"
        val storedToken = mock(com.example.authservice.infrastructure.persistence.token.RefreshTokenEntity::class.java)

        `when`(tokenHashService.sha256(refreshToken)).thenReturn(tokenHash)
        `when`(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(storedToken)
        `when`(storedToken.revoked).thenReturn(false)

        authService.logout(com.example.authservice.api.auth.dto.LogoutRequest(refreshToken))

        verify(storedToken).revoked = true
        verify(refreshTokenRepository).save(storedToken)
    }
}
