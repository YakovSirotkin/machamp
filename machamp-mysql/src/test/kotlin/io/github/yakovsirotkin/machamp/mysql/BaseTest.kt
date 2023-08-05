package io.github.yakovsirotkin.machamp.mysql

import org.junit.After
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.jdbc.JdbcTestUtils
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container

open class BaseTest(
    val jdbcTemplate: JdbcTemplate
) {
    companion object {
        @Container
        val mySQLContainer = MySQLContainer("mysql:8.1.0")
            .withInitScript("sql/001-init.sql")
            .withReuse(true)

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl)
            registry.add("spring.datasource.jdbcUrl", mySQLContainer::getJdbcUrl)
            registry.add("spring.datasource.username", mySQLContainer::getUsername)
            registry.add("spring.datasource.password", mySQLContainer::getPassword)
        }
    }

    @After
    open fun tearDown() {
        JdbcTestUtils.deleteFromTables(
            jdbcTemplate, "async_task"
        )
    }
}
