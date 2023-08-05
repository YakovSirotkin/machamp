package io.github.yakovsirotkin.machamp.mysql

import io.github.yakovsirotkin.machamp.AdminController
import io.github.yakovsirotkin.machamp.AsyncTaskHandler
import io.github.yakovsirotkin.machamp.AsyncTaskProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class TestConfiguration @Autowired constructor(
    private val asyncTaskDao: MySqlAsyncTaskDao,
    private val taskHandlers: List<AsyncTaskHandler>,
    @Value("\${machamp.processor.threads:10}")
    private val threadsCount: Int,
){
    @Bean
    open fun getAsyncTaskProcessor() : AsyncTaskProcessor {
        return AsyncTaskProcessor(asyncTaskDao, threadsCount, false, taskHandlers)
    }

    @Bean
    open fun getAdminController() : AdminController {
        return AdminController(asyncTaskDao)
    }
}
