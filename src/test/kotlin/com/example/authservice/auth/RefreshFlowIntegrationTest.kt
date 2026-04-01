package com.example.authservice.auth

import com.example.authservice.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*

class RefreshFlowIntegrationTest : AbstractIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should refresh token and revoke old refresh token`() {
        registerUser("refresh@example.com", "StrongPass123")

        val loginResponse = login("refresh@example.com", "StrongPass123")
        val refreshToken = extractJsonValue(loginResponse.body!!, "refreshToken")

        val refreshBody = """
            {
              "refreshToken": "$refreshToken"
            }
        """.trimIndent()

        val refreshResponse = postJson("/api/v1/auth/refresh", refreshBody)
        assertThat(refreshResponse.statusCode).isEqualTo(HttpStatus.OK)

        val newRefreshToken = extractJsonValue(refreshResponse.body!!, "refreshToken")
        assertThat(newRefreshToken).isNotBlank
        assertThat(newRefreshToken).isNotEqualTo(refreshToken)

        val secondUseOldTokenResponse = postJson("/api/v1/auth/refresh", refreshBody)
        assertThat(secondUseOldTokenResponse.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should logout token successfully`() {
        registerUser("logout@example.com", "StrongPass123")

        val loginResponse = login("logout@example.com", "StrongPass123")
        val refreshToken = extractJsonValue(loginResponse.body!!, "refreshToken")

        val logoutBody = """
            {
              "refreshToken": "$refreshToken"
            }
        """.trimIndent()

        val logoutResponse = postJson("/api/v1/auth/logout", logoutBody)
        assertThat(logoutResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(logoutResponse.body).contains("Logged out successfully")

        val reuseResponse = postJson("/api/v1/auth/refresh", logoutBody)
        assertThat(reuseResponse.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should return unauthorized for invalid refresh token`() {
        val refreshBody = """
            {
              "refreshToken": "invalid-token"
            }
        """.trimIndent()

        val response = postJson("/api/v1/auth/refresh", refreshBody)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body).contains("INVALID_REFRESH_TOKEN")
    }

    private fun registerUser(email: String, password: String) {
        val body = """
            {
              "email": "$email",
              "password": "$password"
            }
        """.trimIndent()

        val response = postJson("/api/v1/auth/register", body)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    private fun login(email: String, password: String): ResponseEntity<String> {
        val body = """
            {
              "email": "$email",
              "password": "$password"
            }
        """.trimIndent()

        val response = postJson("/api/v1/auth/login", body)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        return response
    }

    private fun postJson(path: String, body: String): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val response = restTemplate.postForEntity(
            url(path),
            HttpEntity(body, headers),
            String::class.java
        )

        if (!response.statusCode.is2xxSuccessful) {
            println("[DEBUG_LOG] Request to $path failed with status ${response.statusCode}: ${response.body}")
        }
        return response
    }

    private fun url(path: String): String = "http://localhost:$port$path"

    private fun extractJsonValue(json: String, field: String): String {
        val regex = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
            ?: error("Field '$field' not found in JSON: $json")
    }
}