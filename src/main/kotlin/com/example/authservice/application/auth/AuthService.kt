package com.example.authservice.application.auth

import com.example.authservice.api.auth.dto.AuthResponse
import com.example.authservice.api.auth.dto.LoginRequest
import com.example.authservice.api.auth.dto.RegisterRequest
import com.example.authservice.api.auth.dto.UserShortResponse
import com.example.authservice.common.exception.ConflictException
import com.example.authservice.common.exception.UnauthorizedException
import com.example.authservice.domain.user.UserRole
import com.example.authservice.domain.user.UserStatus
import com.example.authservice.infrastructure.persistence.user.UserEntity
import com.example.authservice.infrastructure.persistence.user.UserRepository
import com.example.authservice.infrastructure.security.TokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenProvider: TokenProvider
) {

    fun register(request: RegisterRequest): AuthResponse {
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

        return AuthResponse(
            user = savedUser.toUserShortResponse(),
            accessToken = tokenProvider.generateAccessToken(savedUser),
            refreshToken = tokenProvider.generateRefreshToken(savedUser)
        )
    }

    fun login(request: LoginRequest): AuthResponse {
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

        return AuthResponse(
            user = user.toUserShortResponse(),
            accessToken = tokenProvider.generateAccessToken(user),
            refreshToken = tokenProvider.generateRefreshToken(user)
        )
    }

    private fun UserEntity.toUserShortResponse(): UserShortResponse {
        return UserShortResponse(
            id = this.id,
            email = this.email,
            role = this.role.name
        )
    }
}