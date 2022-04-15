package ru.telamon.machamp.springboot

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.telamon.machamp.AsyncTaskDao
import ru.telamon.machamp.AsyncTaskHandler
import ru.telamon.machamp.AsyncTaskProcessor

/**
 * AutoConfigure
 */
@Configuration
@EnableConfigurationProperties(MachampProperties::class)
@ConditionalOnClass(AsyncTaskProcessor::class)
open class AsyncTaskProcessorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AsyncTaskProcessor::class)
    open fun asyncTaskProcessor(
        taskHandlers: List<AsyncTaskHandler>,
        machampProperties: MachampProperties,
        asyncTaskDao: AsyncTaskDao
    ): AsyncTaskProcessor {
        return AsyncTaskProcessor(
            asyncTaskDao,
            machampProperties.processor.threads, taskHandlers
        )
    }
}