package com.example.authservice.infrastructure.persistence.user

import com.example.authservice.domain.user.UserRole
import com.example.authservice.domain.user.UserStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "users")
class UserEntity(

    @Id
    val id: UUID,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: UserStatus,

    @Column(name = "email_verified", nullable = false)
    val emailVerified: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant
) {
    protected constructor() : this(
        id = UUID.randomUUID(),
        email = "",
        passwordHash = "",
        role = UserRole.USER,
        status = UserStatus.ACTIVE,
        emailVerified = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}