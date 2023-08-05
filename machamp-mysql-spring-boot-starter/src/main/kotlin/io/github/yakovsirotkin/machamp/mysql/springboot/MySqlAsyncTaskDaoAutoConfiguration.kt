package io.github.yakovsirotkin.machamp.client.sqlserver.springboot

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.yakovsirotkin.machamp.mysql.MySqlAsyncTaskDao
import io.github.yakovsirotkin.machamp.springboot.MachampProperties
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
@EnableConfigurationProperties(MachampProperties::class)
@ConditionalOnClass(MySqlAsyncTaskDao::class)
open class MySqlAsyncTaskDaoAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(MySqlAsyncTaskDao::class)
    open fun oracleAsyncTaskDao(
        jdbcTemplate: JdbcTemplate,
        objectMapper: ObjectMapper,
        machampProperties: MachampProperties
    ): MySqlAsyncTaskDao {
        return MySqlAsyncTaskDao(jdbcTemplate, objectMapper,
            machampProperties.priority.enabled,
            machampProperties.priority.defaultValue,
            machampProperties.taskTable
        )
    }
}
