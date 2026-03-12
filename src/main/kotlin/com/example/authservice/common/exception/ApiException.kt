package com.example.authservice.common.exception

import org.springframework.http.HttpStatus

open class ApiException(
    val code: String,
    val status: HttpStatus,
    override val message: String
) : RuntimeException(message)