package ru.telamon.machamp

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Admin interface endpoints for controlling async tasks queue.
 */
@RestController
@RequestMapping("/machamp/admin")
class AdminController(private val asyncTaskDao: AsyncTaskDao) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/tasks")
    fun listTask(@RequestParam(defaultValue = "1000") limit: Int): List<AsyncTaskDto> {
        return asyncTaskDao.tasks(limit)
    }

    @PostMapping("/add")
    fun addTask(@RequestBody createAsyncTaskDto: CreateAsyncTaskDto): TaskIdDto {
        return TaskIdDto(asyncTaskDao.createTask(createAsyncTaskDto.taskType, createAsyncTaskDto.description))
    }

    @PostMapping("/process/{taskId}/{attempt}")
    fun processNow(
        @PathVariable("taskId") taskId: Long,
        @PathVariable("attempt") expectedAttempt: Int
    ): UpdateResponseDto {
        logger.info("Process task $taskId now if attempt is $expectedAttempt")
        val updated = asyncTaskDao.processNow(taskId, expectedAttempt)
        logger.info("$updated rows were marked for processing now for taskId = $taskId and expected attempt = $expectedAttempt")
        return UpdateResponseDto(updated)
    }

    @DeleteMapping("/task/{taskId}")
    fun deleteTask(@PathVariable taskId: Long) {
        logger.info("Deleting task $taskId")
        asyncTaskDao.deleteTask(taskId)
    }
}