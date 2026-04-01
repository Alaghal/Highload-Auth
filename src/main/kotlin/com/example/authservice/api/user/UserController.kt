package com.example.authservice.api.user

import com.example.authservice.api.user.dto.UserMeResponse
import com.example.authservice.application.user.UserService
import com.example.authservice.infrastructure.security.AuthenticatedUser
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    fun getMe(
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): UserMeResponse {
        return userService.getMe(principal.id)
    }
}
