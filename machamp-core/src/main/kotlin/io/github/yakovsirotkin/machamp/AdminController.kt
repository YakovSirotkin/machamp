package io.github.yakovsirotkin.machamp

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
@Tag(name = "MachampAdminController")
@SecurityRequirement(name = "machampAdmin")
@ConditionalOnProperty(prefix = "machamp", name = ["adminEnabled"], havingValue = "true", matchIfMissing = false)
class AdminController(private val asyncTaskDao: AsyncTaskDao) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Operation(summary = "List of pending tasks")
    @ApiResponse(
        responseCode = "200",
        description = "The response for the user request",
        content = [
            Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = AsyncTaskDto::class))
            )]
    )
    @GetMapping("/tasks")
    fun listTask(@RequestParam(defaultValue = "1000") limit: Int): List<AsyncTaskDto> {
        return asyncTaskDao.tasks(limit)
    }

    @Operation(summary = "Async task addition")
    @ApiResponse(
        responseCode = "200",
        description = "Task was added",
        content = [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = TaskIdDto::class)
            )]
    )
    @PostMapping("/add")
    fun addTask(@RequestBody createAsyncTaskDto: CreateAsyncTaskDto): TaskIdDto {
        return TaskIdDto(asyncTaskDao.createTask(createAsyncTaskDto.taskType, createAsyncTaskDto.description))
    }

    @Operation(summary = "Schedule async task for immediate processing")
    @ApiResponse(
        responseCode = "200",
        description = "Returns number of task that were set to immediate processing, 0 or 1",
        content = [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = UpdateResponseDto::class)
            )]
    )
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

    @Operation(summary = "Delete async task")
    @ApiResponse(
        responseCode = "200",
        description = "Returns number of task that were deleted, 0 or 1",
        content = [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = UpdateResponseDto::class)
            )]
    )
    @DeleteMapping("/task/{taskId}")
    fun deleteTask(@PathVariable taskId: Long) {
        logger.info("Deleting task $taskId")
        asyncTaskDao.deleteTask(taskId)
    }
}