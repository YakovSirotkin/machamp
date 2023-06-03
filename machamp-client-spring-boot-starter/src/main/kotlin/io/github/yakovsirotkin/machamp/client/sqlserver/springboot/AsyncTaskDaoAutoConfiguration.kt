package io.github.yakovsirotkin.machamp.client.sqlserver.springboot

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.yakovsirotkin.machamp.AsyncTaskDao
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate

import org.springframework.beans.factory.annotation.Qualifier


/**
 * AutoConfigure
 */
@Configuration
@EnableConfigurationProperties(ClientMachampProperties::class)
@ConditionalOnClass(AsyncTaskDao::class)
open class AsyncTaskDaoAutoConfiguration {

    @Bean
    @Qualifier
    @ConditionalOnMissingBean(AsyncTaskDao::class)
    open fun asyncTaskDao(
        jdbcTemplate: JdbcTemplate,
        objectMapper: ObjectMapper,
        machampProperties: ClientMachampProperties
    ): AsyncTaskDao {
        return AsyncTaskDao(jdbcTemplate, objectMapper,
            true,
            machampProperties.priority.defaultValue,
            machampProperties.taskTable,
        )
    }
}
