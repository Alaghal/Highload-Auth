package com.example.authservice.auth

import com.example.authservice.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*

class AuthFlowIntegrationTest : AbstractIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should register user successfully`() {
        val requestBody = """
            {
              "email": "user@example.com",
              "password": "StrongPass123"
            }
        """.trimIndent()

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val request = HttpEntity(requestBody, headers)

        val response = restTemplate.postForEntity(
            url("/api/v1/auth/register"),
            request,
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).contains("accessToken")
        assertThat(response.body).contains("refreshToken")
        assertThat(response.body).contains("user@example.com")
    }

    @Test
    fun `should login and access protected me endpoint`() {
        registerUser("john@example.com", "StrongPass123")

        val loginBody = """
            {
              "email": "john@example.com",
              "password": "StrongPass123"
            }
        """.trimIndent()

        val loginResponse = postJson("/api/v1/auth/login", loginBody)

        assertThat(loginResponse.statusCode).isEqualTo(HttpStatus.OK)
        val accessToken = extractJsonValue(loginResponse.body!!, "accessToken")
        assertThat(accessToken).isNotBlank

        val headers = HttpHeaders()
        headers.setBearerAuth(accessToken)

        val meResponse = restTemplate.exchange(
            url("/api/v1/users/me"),
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            String::class.java
        )

        assertThat(meResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(meResponse.body).contains("john@example.com")
        assertThat(meResponse.body).contains("USER")
    }

    @Test
    fun `should return unauthorized for protected endpoint without token`() {
        val response = restTemplate.getForEntity(
            url("/api/v1/users/me"),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    private fun registerUser(email: String, password: String) {
        val requestBody = """
            {
              "email": "$email",
              "password": "$password"
            }
        """.trimIndent()

        val response = postJson("/api/v1/auth/register", requestBody)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    private fun postJson(path: String, body: String): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return restTemplate.postForEntity(
            url(path),
            HttpEntity(body, headers),
            String::class.java
        )
    }

    private fun url(path: String): String = "http://localhost:$port$path"

    private fun extractJsonValue(json: String, field: String): String {
        val regex = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
            ?: error("Field '$field' not found in JSON: $json")
    }
}