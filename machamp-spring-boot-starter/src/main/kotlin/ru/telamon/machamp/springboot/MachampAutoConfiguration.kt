package ru.telamon.machamp.springboot

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import ru.telamon.machamp.AsyncTaskDao
import ru.telamon.machamp.AsyncTaskHandler
import ru.telamon.machamp.AsyncTaskProcessor

/**
 * AutoConfigure
 */
@Configuration
@EnableConfigurationProperties(MachampProperties::class)
@ConditionalOnClass(AsyncTaskProcessor::class)
open class MachampAutoConfiguration @Autowired constructor(
    val jdbcTemplate: JdbcTemplate,
    val objectMapper: ObjectMapper,
    val taskHandlers: List<AsyncTaskHandler>,
    val machampProperties: MachampProperties
) {

    @Bean
    @ConditionalOnMissingBean(AsyncTaskDao::class)
    open fun asyncTaskDao(): AsyncTaskDao {
        return AsyncTaskDao(jdbcTemplate, objectMapper, machampProperties.priority.enabled)
    }

    @Bean
    @ConditionalOnMissingBean(AsyncTaskProcessor::class)
    open fun asyncTaskProcessor(asyncTaskDao: AsyncTaskDao): AsyncTaskProcessor {
        return AsyncTaskProcessor(asyncTaskDao,
            machampProperties.processor.threads, taskHandlers)
    }
}