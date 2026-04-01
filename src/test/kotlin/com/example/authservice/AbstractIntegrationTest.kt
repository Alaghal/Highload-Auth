package com.example.authservice

import com.example.authservice.infrastructure.persistence.token.RefreshTokenRepository
import com.example.authservice.infrastructure.persistence.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var refreshTokenRepository: RefreshTokenRepository

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    companion object {
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .withDatabaseName("test_auth_db")
            .withUsername("test_user")
            .withPassword("test_password")

        init {
            postgres.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("app.jwt.secret") { "test-secret-key-test-secret-key-test-secret-key-123456" }
            registry.add("app.jwt.access-token-expiration-minutes") { "15" }
            registry.add("app.jwt.refresh-token-expiration-days") { "7" }
            registry.add("management.tracing.sampling.probability") { "0" }
            registry.add("management.otlp.tracing.endpoint") { "http://localhost:4318" } // meaningless but avoids default
            registry.add("spring.jpa.properties.hibernate.connection.pool_size") { "10" }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "20" }
            registry.add("spring.datasource.hikari.minimum-idle") { "5" }
        }
    }
}