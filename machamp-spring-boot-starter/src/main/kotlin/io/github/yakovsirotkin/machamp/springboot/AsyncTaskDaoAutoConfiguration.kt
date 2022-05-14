package io.github.yakovsirotkin.machamp.springboot

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import io.github.yakovsirotkin.machamp.AsyncTaskDao


/**
 * AutoConfigure
 */
@Configuration
@EnableConfigurationProperties(MachampProperties::class)
@ConditionalOnClass(AsyncTaskDao::class)
open class AsyncTaskDaoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AsyncTaskDao::class)
    open fun asyncTaskDao(
        jdbcTemplate: JdbcTemplate,
        objectMapper: ObjectMapper,
        machampProperties: MachampProperties
    ): AsyncTaskDao {
        return AsyncTaskDao(jdbcTemplate, objectMapper, machampProperties.priority.enabled)
    }
}
