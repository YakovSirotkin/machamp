package io.github.yakovsirotkin.machamp

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
    properties = [
        "machamp.processor.threads=1"
    ]
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PriorityTest @Autowired constructor(
    private val priorityAsyncTaskHandler: PriorityAsyncTaskHandler,
    private val asyncTaskDao: AsyncTaskDao,
    private val transactionTemplate: TransactionTemplate,
    jdbcTemplate: JdbcTemplate
) :
    BaseTest(jdbcTemplate) {

    @Test
    fun priorityTest() {
        val n = 100
        transactionTemplate.execute {
            for (i in 1..n) {
                val priority = (i + n / 2) % n + 1
                asyncTaskDao.createTask(
                    priorityAsyncTaskHandler.getType(),
                    "{\"value\": $priority}",
                    priority
                )
            }
        }
        val processed = priorityAsyncTaskHandler.getProcessed()
        runBlocking {
            while (processed.size < n) {
                delay(1000)
            }
        }
        for (i in 1..n) {
            assertEquals(i, processed[i - 1], "Task with priority $i should be processed here, " +
                    " not ${processed[i - 1]}")
        }
    }
}

@Component
open class PriorityAsyncTaskHandler : AsyncTaskHandler {

    private val processed = ArrayList<Int>()

    override fun getType(): String {
        return "priority"
    }

    override fun process(asyncTask: AsyncTask): Boolean {
        processed.add(asyncTask.description.get("value").asInt())
        return true
    }

    fun getProcessed(): ArrayList<Int> {
        return processed
    }
}