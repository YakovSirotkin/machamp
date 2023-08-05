package io.github.yakovsirotkin.machamp.oracle

import org.junit.After
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.jdbc.JdbcTestUtils
import org.testcontainers.containers.OracleContainer
import org.testcontainers.junit.jupiter.Container

open class BaseTest(
    val jdbcTemplate: JdbcTemplate
) {
    companion object {
        @Container
        val oracle = OracleContainer("gvenzl/oracle-xe:18.4.0-slim")
            .withInitScript("sql/001-init.sql")
            .withReuse(true)

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", oracle::getJdbcUrl)
            registry.add("spring.datasource.jdbcUrl", oracle::getJdbcUrl)
            registry.add("spring.datasource.username", oracle::getUsername)
            registry.add("spring.datasource.password", oracle::getPassword)
        }
    }

    @After
    open fun tearDown() {
        JdbcTestUtils.deleteFromTables(
            jdbcTemplate, "async_task"
        )
    }
}
