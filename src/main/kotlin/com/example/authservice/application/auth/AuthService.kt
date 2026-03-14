package com.example.authservice.application.auth

import com.example.authservice.api.auth.dto.AuthResponse
import com.example.authservice.api.auth.dto.LoginRequest
import com.example.authservice.api.auth.dto.LogoutRequest
import com.example.authservice.api.auth.dto.MessageResponse
import com.example.authservice.api.auth.dto.RefreshRequest
import com.example.authservice.api.auth.dto.RegisterRequest
import com.example.authservice.api.auth.dto.UserShortResponse
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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenProvider: TokenProvider,
    private val jwtTokenProvider: JwtTokenProvider,
    private val tokenHashService: TokenHashService
) {

    @Transactional
    fun register(request: RegisterRequest, httpRequest: HttpServletRequest): AuthResponse {
        val normalizedEmail = request.email.trim().lowercase()

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ConflictException(
                message = "User with email '$normalizedEmail' already exists",
                code = "EMAIL_ALREADY_EXISTS"
            )
        }

        val now = Instant.now()

        val user = UserEntity(
            id = UUID.randomUUID(),
            email = normalizedEmail,
            passwordHash = passwordEncoder.encode(request.password),
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            emailVerified = false,
            createdAt = now,
            updatedAt = now
        )

        val savedUser = userRepository.save(user)
        val accessToken = tokenProvider.generateAccessToken(savedUser)
        val refreshToken = tokenProvider.generateRefreshToken(savedUser)

        persistRefreshToken(savedUser, refreshToken, httpRequest)

        return AuthResponse(
            user = savedUser.toUserShortResponse(),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    @Transactional
    fun login(request: LoginRequest, httpRequest: HttpServletRequest): AuthResponse {
        val normalizedEmail = request.email.trim().lowercase()

        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw UnauthorizedException(
                message = "Invalid email or password",
                code = "INVALID_CREDENTIALS"
            )

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw UnauthorizedException(
                message = "Invalid email or password",
                code = "INVALID_CREDENTIALS"
            )
        }

        if (user.status != UserStatus.ACTIVE) {
            throw UnauthorizedException(
                message = "User account is not active",
                code = "USER_NOT_ACTIVE"
            )
        }

        val accessToken = tokenProvider.generateAccessToken(user)
        val refreshToken = tokenProvider.generateRefreshToken(user)

        persistRefreshToken(user, refreshToken, httpRequest)

        return AuthResponse(
            user = user.toUserShortResponse(),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    @Transactional
    fun refresh(request: RefreshRequest, httpRequest: HttpServletRequest): AuthResponse {
        val rawRefreshToken = request.refreshToken

        if (!jwtTokenProvider.isRefreshToken(rawRefreshToken)) {
            throw UnauthorizedException(
                message = "Invalid refresh token",
                code = "INVALID_REFRESH_TOKEN"
            )
        }

        val tokenHash = tokenHashService.sha256(rawRefreshToken)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw UnauthorizedException(
                message = "Refresh token not found",
                code = "REFRESH_TOKEN_NOT_FOUND"
            )

        if (storedToken.revoked) {
            throw UnauthorizedException(
                message = "Refresh token has been revoked",
                code = "REFRESH_TOKEN_REVOKED"
            )
        }

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            throw UnauthorizedException(
                message = "Refresh token has expired",
                code = "REFRESH_TOKEN_EXPIRED"
            )
        }

        val user = userRepository.findById(storedToken.userId)
            .orElseThrow {
                UnauthorizedException(
                    message = "User for refresh token not found",
                    code = "USER_NOT_FOUND"
                )
            }

        storedToken.revoked = true
        refreshTokenRepository.save(storedToken)

        val newAccessToken = tokenProvider.generateAccessToken(user)
        val newRefreshToken = tokenProvider.generateRefreshToken(user)

        persistRefreshToken(user, newRefreshToken, httpRequest)

        return AuthResponse(
            user = user.toUserShortResponse(),
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun logout(request: LogoutRequest): MessageResponse {
        val tokenHash = tokenHashService.sha256(request.refreshToken)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)

        if (storedToken != null && !storedToken.revoked) {
            storedToken.revoked = true
            refreshTokenRepository.save(storedToken)
        }

        return MessageResponse("Logged out successfully")
    }

    @Transactional
    fun logoutAll(userId: UUID): MessageResponse {
        val tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)

        tokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(tokens)

        return MessageResponse("Logged out from all sessions")
    }

    private fun persistRefreshToken(
        user: UserEntity,
        rawRefreshToken: String,
        httpRequest: HttpServletRequest
    ) {
        val tokenHash = tokenHashService.sha256(rawRefreshToken)

        val refreshTokenEntity = RefreshTokenEntity(
            id = UUID.randomUUID(),
            userId = user.id,
            tokenHash = tokenHash,
            expiresAt = jwtTokenProvider.extractExpiration(rawRefreshToken),
            revoked = false,
            createdAt = Instant.now(),
            userAgent = httpRequest.getHeader("User-Agent"),
            ipAddress = extractClientIp(httpRequest)
        )

        refreshTokenRepository.save(refreshTokenEntity)
    }

    private fun extractClientIp(request: HttpServletRequest): String? {
        return request.getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?: request.remoteAddr
    }

    private fun UserEntity.toUserShortResponse(): UserShortResponse {
        return UserShortResponse(
            id = this.id,
            email = this.email,
            role = this.role.name
        )
    }
}