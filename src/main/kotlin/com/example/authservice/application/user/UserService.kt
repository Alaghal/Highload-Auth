package com.example.authservice.application.user

import com.example.authservice.api.user.dto.UserMeResponse
import com.example.authservice.common.exception.UnauthorizedException
import com.example.authservice.infrastructure.persistence.user.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getMe(userId: UUID): UserMeResponse {
        val user = userRepository.findById(userId)
            .orElseThrow {
                UnauthorizedException(
                    message = "Authenticated user not found",
                    code = "USER_NOT_FOUND"
                )
            }

        return UserMeResponse(
            id = user.id,
            email = user.email,
            role = user.role.name,
            emailVerified = user.emailVerified,
            createdAt = user.createdAt
        )
    }
}