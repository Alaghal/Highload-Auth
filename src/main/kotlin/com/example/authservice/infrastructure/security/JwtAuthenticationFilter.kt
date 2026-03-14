package com.example.authservice.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        if (!jwtTokenProvider.isTokenValid(token)) {
            filterChain.doFilter(request, response)
            return
        }

        if (jwtTokenProvider.extractType(token) != "access") {
            filterChain.doFilter(request, response)
            return
        }

        val userId = jwtTokenProvider.extractUserId(token)
        val email = jwtTokenProvider.extractEmail(token) ?: ""
        val role = jwtTokenProvider.extractRole(token) ?: "USER"

        val principal = AuthenticatedUser(
            id = userId,
            email = email,
            role = role
        )

        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.authorities
        )

        SecurityContextHolder.getContext().authentication = authentication
        filterChain.doFilter(request, response)
    }
}