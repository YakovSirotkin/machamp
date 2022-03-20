package ru.telamon.machamp

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CleanupDuplicatesTest @Autowired constructor(
    private val duplicateTaskHandler: DuplicateTaskHandler,
    private val asyncTaskDao: AsyncTaskDao,
    jdbcTemplate: JdbcTemplate
) :
    BaseTest(jdbcTemplate) {

    @Test
    fun cleanDuplicatesTest() {
        val threads = 10
        val n = 100L
        runBlocking {
            for (mod in 0 until threads) {
                launch {
                    for (i in 0 until n) {
                        duplicateTaskHandler.create()
                    }
                }
            }
        }
        runBlocking {
            delay(1000)
        }
        val totalTasks = n * threads
        assertEquals(totalTasks, duplicateTaskHandler.created.get(), "Should create all tasks")
        val tasks = asyncTaskDao.tasks(100)
        println(tasks)
        assertEquals(0, tasks.size, "Should be no open tasks")
        val totalProcessed = duplicateTaskHandler.total.get()
        assertTrue(totalTasks > totalProcessed, "Should process only some tasks")
        assertTrue(totalProcessed > 0, "Task should be processed")
        println("Processed $totalProcessed from $totalTasks")
    }
}

@Component
open class DuplicateTaskHandler constructor(val asyncTaskDao: AsyncTaskDao) : AsyncTaskHandler {

    val total = AtomicLong(0)

    val created = AtomicLong(0)

    override fun getType(): String {
        return "duplicate"
    }

    override fun process(asyncTask: AsyncTask): Boolean {
        total.incrementAndGet()
        return true
    }

    fun create() {
        val property = "bob"
        val value = 42L
        val createdTaskId = asyncTaskDao.createTask(getType(), "{\"$property\": $value}")
        created.incrementAndGet()
        asyncTaskDao.deleteDuplicateTask(createdTaskId, getType(), property, value)
    }
}