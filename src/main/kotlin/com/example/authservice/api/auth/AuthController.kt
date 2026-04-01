package com.example.authservice.api.auth

import com.example.authservice.api.auth.dto.AuthResponse
import com.example.authservice.api.auth.dto.LoginRequest
import com.example.authservice.api.auth.dto.LogoutRequest
import com.example.authservice.api.auth.dto.MessageResponse
import com.example.authservice.api.auth.dto.RefreshRequest
import com.example.authservice.api.auth.dto.RegisterRequest
import com.example.authservice.application.auth.AuthService
import com.example.authservice.infrastructure.security.AuthenticatedUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    private companion object {
        private val log = LoggerFactory.getLogger(AuthController::class.java)
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: RegisterRequest,
        httpRequest: HttpServletRequest
    ): AuthResponse {
        log.info("Register request received for email={}", request.email)
        return authService.register(request, httpRequest)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): AuthResponse {
        log.info("Login request received for email={}", request.email)
        return authService.login(request, httpRequest)
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
        httpRequest: HttpServletRequest
    ): AuthResponse {
        log.info("Refresh token request received")
        return authService.refresh(request, httpRequest)
    }

    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody request: LogoutRequest
    ): MessageResponse {
        log.info("Logout request received")
        return authService.logout(request)
    }

    @PostMapping("/logout-all")
    fun logoutAll(
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): MessageResponse {
        log.info("Logout-all request received for userId={}", principal.id)
        return authService.logoutAll(principal.id)
    }
}
