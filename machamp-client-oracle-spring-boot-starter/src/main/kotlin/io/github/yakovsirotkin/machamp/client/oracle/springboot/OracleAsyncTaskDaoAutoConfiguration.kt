package io.github.yakovsirotkin.machamp.client.sqlserver.springboot

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import io.github.yakovsirotkin.machamp.oracle.OracleAsyncTaskDao
import org.springframework.beans.factory.annotation.Qualifier

/**
 * AutoConfigure
 */
@Configuration
@EnableConfigurationProperties(OracleClientMachampProperties::class)
@ConditionalOnClass(OracleAsyncTaskDao::class)
open class OracleAsyncTaskDaoAutoConfiguration {

    @Bean
    @Qualifier
    @ConditionalOnMissingBean(OracleAsyncTaskDao::class)
    open fun asyncTaskDao(
        jdbcTemplate: JdbcTemplate,
        objectMapper: ObjectMapper,
        machampProperties: OracleClientMachampProperties
    ): OracleAsyncTaskDao {
        return OracleAsyncTaskDao(jdbcTemplate, objectMapper,
            true,
            machampProperties.priority.defaultValue,
            machampProperties.taskTable,
            machampProperties.taskSequence
        )
    }
}
