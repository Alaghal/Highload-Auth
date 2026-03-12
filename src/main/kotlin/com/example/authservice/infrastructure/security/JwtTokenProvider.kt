package com.example.authservice.infrastructure.security

import com.example.authservice.config.JwtProperties
import com.example.authservice.infrastructure.persistence.user.UserEntity
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) : TokenProvider {

    private val key: SecretKey = Keys.hmacShaKeyFor(
        normalizeSecret(jwtProperties.secret)
    )

    override fun generateAccessToken(user: UserEntity): String {
        val now = Instant.now()
        val expiresAt = now.plus(jwtProperties.accessTokenExpirationMinutes, ChronoUnit.MINUTES)

        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact()
    }

    override fun generateRefreshToken(user: UserEntity): String {
        val now = Instant.now()
        val expiresAt = now.plus(jwtProperties.refreshTokenExpirationDays, ChronoUnit.DAYS)

        return Jwts.builder()
            .subject(user.id.toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact()
    }

    fun extractUserId(token: String): UUID {
        val claims = extractAllClaims(token)
        return UUID.fromString(claims.subject)
    }

    fun extractEmail(token: String): String? {
        return extractAllClaims(token)["email"] as? String
    }

    fun extractRole(token: String): String? {
        return extractAllClaims(token)["role"] as? String
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            val claims = extractAllClaims(token)
            claims.expiration.after(Date())
        } catch (_: Exception) {
            false
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun normalizeSecret(secret: String): ByteArray {
        return if (isBase64(secret)) {
            Decoders.BASE64.decode(secret)
        } else {
            secret.toByteArray()
        }
    }

    private fun isBase64(value: String): Boolean {
        return try {
            Decoders.BASE64.decode(value)
            true
        } catch (_: Exception) {
            false
        }
    }
}