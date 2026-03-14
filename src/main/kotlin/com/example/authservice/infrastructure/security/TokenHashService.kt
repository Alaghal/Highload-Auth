package com.example.authservice.infrastructure.security

import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class TokenHashService {

    fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}