package io.github.yakovsirotkin.machamp.springboot

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionTemplate
import io.github.yakovsirotkin.machamp.AsyncTaskDao
import io.github.yakovsirotkin.machamp.AsyncTaskHandler
import io.github.yakovsirotkin.machamp.AsyncTaskProcessor

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
        asyncTaskDao: AsyncTaskDao, transactionTemplate: TransactionTemplate
    ): AsyncTaskProcessor {
        return AsyncTaskProcessor(
            asyncTaskDao,
            machampProperties.processor.threads, taskHandlers
        )
    }
}