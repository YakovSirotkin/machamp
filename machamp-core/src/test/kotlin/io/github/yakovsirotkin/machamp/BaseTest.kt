package io.github.yakovsirotkin.machamp

import org.junit.After
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.jdbc.JdbcTestUtils
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

open class BaseTest constructor(val jdbcTemplate: JdbcTemplate) {
    companion object {
        @Container
        private val postgresDB: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:12")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("sql/001-init.sql")
            .withReuse(true)

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresDB::getJdbcUrl)
            registry.add("spring.datasource.jdbcUrl", postgresDB::getJdbcUrl)
            registry.add("spring.datasource.username", postgresDB::getUsername)
            registry.add("spring.datasource.password", postgresDB::getPassword)
        }
    }

    @After
    open fun tearDown() {
        JdbcTestUtils.deleteFromTables(
            jdbcTemplate, "async_task")
    }
}