package io.github.yakovsirotkin.machamp.sqlserver.springboot

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.yakovsirotkin.machamp.oracle.OracleAsyncTaskDao
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate

import org.springframework.context.annotation.Primary

/**
 * AutoConfigure
 */
@Configuration
@EnableConfigurationProperties(OracleMachampProperties::class)
@ConditionalOnClass(OracleAsyncTaskDao::class)
open class OracleAsyncTaskDaoAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(OracleAsyncTaskDao::class)
    open fun oracleAsyncTaskDao(
        jdbcTemplate: JdbcTemplate,
        objectMapper: ObjectMapper,
        oracleMachampProperties: OracleMachampProperties
    ): OracleAsyncTaskDao {
        return OracleAsyncTaskDao(jdbcTemplate, objectMapper,
            oracleMachampProperties.priority.enabled,
            oracleMachampProperties.priority.defaultValue,
            oracleMachampProperties.taskTable,
            oracleMachampProperties.taskSequence
        )
    }
}
