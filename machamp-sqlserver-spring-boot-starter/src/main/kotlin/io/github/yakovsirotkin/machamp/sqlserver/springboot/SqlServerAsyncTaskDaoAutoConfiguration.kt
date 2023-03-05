package io.github.yakovsirotkin.machamp.sqlserver.springboot

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import io.github.yakovsirotkin.machamp.springboot.MachampProperties
import io.github.yakovsirotkin.machamp.sqlserver.SqlServerAsyncTaskDao


/**
 * AutoConfigure
 */
@Configuration
@EnableConfigurationProperties(MachampProperties::class)
@ConditionalOnClass(SqlServerAsyncTaskDao::class)
open class SqlServerAsyncTaskDaoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SqlServerAsyncTaskDaoAutoConfiguration::class)
    open fun asyncTaskDao(
        jdbcTemplate: JdbcTemplate,
        objectMapper: ObjectMapper,
        machampProperties: MachampProperties
    ): SqlServerAsyncTaskDao {
        return SqlServerAsyncTaskDao(jdbcTemplate, objectMapper,
            machampProperties.priority.enabled,
            machampProperties.priority.defaultValue,
            machampProperties.taskTable,
        )
    }
}
