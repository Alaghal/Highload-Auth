package com.example.authservice.common.exception

import org.springframework.http.HttpStatus

class ConflictException(
    message: String,
    code: String = "CONFLICT"
) : ApiException(code, HttpStatus.CONFLICT, message)