package ru.telamon.machamp.conf

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import ru.telamon.machamp.AsyncTaskDao
import ru.telamon.machamp.AsyncTaskHandler
import ru.telamon.machamp.AsyncTaskProcessor


@AutoConfigureOrder
@ConditionalOnProperty(prefix = "machamp", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MachampProperties::class)
@Configuration
open class MachampAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    open fun asyncTaskDao(jdbcTemplate: JdbcTemplate, mapper: ObjectMapper): AsyncTaskDao {
        return AsyncTaskDao(jdbcTemplate, mapper)
    }

    @ConditionalOnMissingBean
    @Bean
    open fun asyncTaskProcessor(
        asyncTaskDao: AsyncTaskDao,
        taskHandlers: List<AsyncTaskHandler>,
        machampProperties: MachampProperties
    ): AsyncTaskProcessor {
        return AsyncTaskProcessor(asyncTaskDao, machampProperties.processorParallelismLevel, taskHandlers)
    }
}