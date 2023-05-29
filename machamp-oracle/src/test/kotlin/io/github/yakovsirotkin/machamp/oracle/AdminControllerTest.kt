package io.github.yakovsirotkin.machamp.oracle

import io.github.yakovsirotkin.machamp.AsyncTaskDto
import io.github.yakovsirotkin.machamp.CreateAsyncTaskDto
import io.github.yakovsirotkin.machamp.TaskIdDto
import io.github.yakovsirotkin.machamp.UpdateResponseDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForObject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "machamp.adminEnabled=true",
    ]
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminControllerTest @Autowired constructor(jdbcTemplate: JdbcTemplate) :
    BaseTest(jdbcTemplate) {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun crudTest() {
        val taskId1 = addTask(1)
        val taskId2 = addTask(2)
        val taskId3 = addTask(3)
        val tasks = getAllTasks()
        assertEquals(3, tasks.size, "All tasks should be returned")
        val only2tasks = restTemplate.getForObject<List<AsyncTaskDto>>("/machamp/admin/tasks?limit=2")
        assertEquals(2, only2tasks?.size, "Limit should work")
        deleteTask(taskId1)
        assertEquals(2, getAllTasks().size, "Delete should work")
        deleteTask(taskId2)
        deleteTask(taskId3)
        assertEquals(0, getAllTasks().size, "Delete should work")
    }

    @Test
    fun processNowTest() {
        val taskId = addTask(1)
        runBlocking {
            delay(2L.toDuration(DurationUnit.SECONDS)) //wait for first processing attempt
        }
        assertEquals(1, getAllTasks()[0].attempt, "Should be only 1 attempt")
        restTemplate.postForObject("/machamp/admin/process/$taskId/1", null, UpdateResponseDto::class.java)
        runBlocking {
            delay(2L.toDuration(DurationUnit.SECONDS)) //wait for second processing attempt
        }
        assertEquals(2, getAllTasks()[0].attempt, "Should be 2 attempt")
        val response =
            restTemplate.postForObject("/machamp/admin/process/$taskId/1", null, UpdateResponseDto::class.java)
        assertEquals(0, response.rowsUpdated, "No rows should be updated")
        runBlocking {
            delay(2L.toDuration(DurationUnit.SECONDS)) //let time to do 3rd attempt
        }
        assertEquals(2, getAllTasks()[0].attempt, "Should be 2 attempt")
        deleteTask(taskId)
        assertEquals(0, getAllTasks().size, "Delete should work")
    }


    private fun getAllTasks(): Array<AsyncTaskDto> {
        return restTemplate.getForObject("/machamp/admin/tasks", Array<AsyncTaskDto>::class.java)!!
    }

    private fun deleteTask(taskId: Long) {
        restTemplate.delete("/machamp/admin/task/$taskId")
    }

    private fun addTask(id: Long): Long =
        restTemplate.postForObject(
            "/machamp/admin/add",
            CreateAsyncTaskDto("TEST", "{\"id\":$id}"), TaskIdDto::class.java
        ).taskId
}
