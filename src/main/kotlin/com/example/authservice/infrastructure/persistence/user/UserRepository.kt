package com.example.authservice.infrastructure.persistence.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<UserEntity, UUID> {

    fun findByEmail(email: String): UserEntity?

    fun existsByEmail(email: String): Boolean
}