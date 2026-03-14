package com.example.authservice.infrastructure.persistence.token

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(

    @Id
    val id: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "token_hash", nullable = false, unique = true)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false)
    var revoked: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "user_agent")
    val userAgent: String? = null,

    @Column(name = "ip_address")
    val ipAddress: String? = null
) {
    protected constructor() : this(
        id = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        tokenHash = "",
        expiresAt = Instant.now(),
        revoked = false,
        createdAt = Instant.now(),
        userAgent = null,
        ipAddress = null
    )
}