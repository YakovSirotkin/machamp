package io.github.yakovsirotkin.machamp.sqlserver

import org.junit.After
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.jdbc.JdbcTestUtils
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.junit.jupiter.Container

open class BaseTest constructor(
    val jdbcTemplate: JdbcTemplate
    ) {
    companion object {
        @Container
        private val mssqlserver: MSSQLServerContainer<*> = MSSQLServerContainer("mcr.microsoft.com/mssql/server:2017-CU12")
            .acceptLicense()
            .withUrlParam("trustServerCertificate", "true")
            .withInitScript("sql/001-init.sql")
            .withReuse(true)

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mssqlserver::getJdbcUrl)
            registry.add("spring.datasource.jdbcUrl", mssqlserver::getJdbcUrl)
            registry.add("spring.datasource.username", mssqlserver::getUsername)
            registry.add("spring.datasource.password", mssqlserver::getPassword)
        }
    }

    @After
    open fun tearDown() {
        JdbcTestUtils.deleteFromTables(
            jdbcTemplate, "async_task")
    }
}
