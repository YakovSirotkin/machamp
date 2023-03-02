package io.github.yakovsirotkin.machamp.sqlserver

import io.github.yakovsirotkin.machamp.AdminController
import io.github.yakovsirotkin.machamp.AsyncTaskHandler
import io.github.yakovsirotkin.machamp.AsyncTaskProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class TestConfiguration @Autowired constructor(
    private val asyncTaskDao: SqlServerAsyncTaskDao,
    private val taskHandlers: List<AsyncTaskHandler>
){
    @Bean
    open fun getAsyncTaskProcessor() : AsyncTaskProcessor {
        return AsyncTaskProcessor(asyncTaskDao, 1, false, taskHandlers)
    }

    @Bean
    open fun getAdminController() : AdminController {
        return AdminController(asyncTaskDao)
    }
}
