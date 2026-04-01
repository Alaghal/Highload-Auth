package com.example.authservice.common.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class RequestIdFilter : OncePerRequestFilter() {

    companion object {
        private const val REQUEST_ID = "requestId"
        private const val HEADER_NAME = "X-Request-Id"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestId = request.getHeader(HEADER_NAME)?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

        MDC.put(REQUEST_ID, requestId)
        response.setHeader(HEADER_NAME, requestId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(REQUEST_ID)
        }
    }
}