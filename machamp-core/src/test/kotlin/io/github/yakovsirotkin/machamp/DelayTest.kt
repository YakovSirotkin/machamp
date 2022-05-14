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
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
    properties = [
        "machamp.processor.threads=1",
    ]
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DelayTest @Autowired constructor(
    private val trivialAsyncTaskHandler: TrivialAsyncTaskHandler,
    private val asyncTaskDao: AsyncTaskDao,
    jdbcTemplate: JdbcTemplate
) :
    BaseTest(jdbcTemplate) {

    @Test
    fun delayTest() {
        asyncTaskDao.createTask(
            trivialAsyncTaskHandler.getType(),
            "{\"value\": 1}",
            1, 5
        )
        runBlocking {
            delay(1000)
        }
        assertEquals(1, asyncTaskDao.tasks(100).size, "Should be one delayed task")
        runBlocking {
            delay(5000)
        }
        assertEquals(0, asyncTaskDao.tasks(100).size, "Task should be processed")
    }
}

@Component
open class TrivialAsyncTaskHandler : AsyncTaskHandler {

    override fun getType(): String {
        return "trivial"
    }

    override fun process(asyncTask: AsyncTask): Boolean {
        return true
    }
}