package com.example.authservice.common.exception

import org.springframework.http.HttpStatus

class UnauthorizedException(
    message: String,
    code: String = "UNAUTHORIZED"
) : ApiException(code, HttpStatus.UNAUTHORIZED, message)