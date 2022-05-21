package io.github.yakovsirotkin.machamp

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

    @Test
    fun priorityAwarenessTest() {
        duplicateTaskHandler.create(100, 1000)
        duplicateTaskHandler.create(1000, 1000)

        assertEquals(2, asyncTaskDao.tasks(100).size, "Should be 2 tasks")
        val lastTaskId = duplicateTaskHandler.create(100, 1000)
        assertEquals(1, asyncTaskDao.tasks(100).size, "Should be 1 task")
        asyncTaskDao.deleteTask(lastTaskId)
    }
}

@Component
open class DuplicateTaskHandler constructor(private val asyncTaskDao: AsyncTaskDao) : AsyncTaskHandler {

    val total = AtomicLong(0)

    val created = AtomicLong(0)

    override fun getType(): String {
        return "duplicate"
    }

    override fun process(asyncTask: AsyncTask): Boolean {
        total.incrementAndGet()
        return true
    }

    fun create(priority: Int = 100, delay: Int = 0): Long {
        val property = "bob"
        val value = 42L
        val createdTaskId = asyncTaskDao.createTask(getType(), "{\"$property\": $value}", priority, delay)
        created.incrementAndGet()
        asyncTaskDao.deleteDuplicateTask(createdTaskId, getType(), property, value, priority)
        return createdTaskId
    }
}