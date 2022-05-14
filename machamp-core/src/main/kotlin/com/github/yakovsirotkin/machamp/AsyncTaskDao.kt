package com.github.yakovsirotkin.machamp

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import java.sql.Statement

/**
 * Database layer
 */
@Component
class AsyncTaskDao @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${machamp.priority.enabled:true}")
    private val priorityEnabled: Boolean
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    /**
     * Creates task in database.
     * @param taskType type of the task
     * @param description data required for task processing
     * @param priority task priority, process tasks with less priority value earlier
     * @param delayInSeconds delay before first attempt in seconds
     * @return taskId in database
     */
    fun createTask(
        taskType: String, description: String,
        priority: Int = 100, delayInSeconds: Int = 0
    ): Long {
        val generatedKeyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({
            val ps = it.prepareStatement(
                "INSERT INTO async_task (task_type, description, priority, process_time) " +
                        " VALUES (?, ?::json, ?, NOW() + ? * interval '1 second' )",
                Statement.RETURN_GENERATED_KEYS
            )
            ps.setString(1, taskType)
            ps.setString(2, description)
            ps.setInt(3, priority)
            ps.setInt(4, delayInSeconds)
            ps
        }, generatedKeyHolder)
        val taskId = generatedKeyHolder.keys!!["task_id"] as Long
        logger.info("Created task $taskId of type $taskType with description $description")
        return taskId
    }

    /**
     * Takes one task with `process_time` in the past from database and moves its `process_time` to the future.
     * @return one [AsyncTask] or null
     */
    fun getTask(): AsyncTask? {
        var response: AsyncTask? = null
        jdbcTemplate.query(
            "UPDATE async_task " +
                    " SET process_time = NOW() + power(2, LEAST(14, attempt)) * interval '1 minute', " +
                    " attempt = LEAST(30000, attempt + 1), " +
                    " taken = NOW() WHERE task_id = " +
                    " (SELECT task_id FROM async_task WHERE process_time < NOW()" +
                    if (priorityEnabled) {
                        " ORDER BY priority ASC "
                    } else {
                        ""
                    } +
                    " LIMIT 1 FOR UPDATE SKIP LOCKED)" +
                    " RETURNING task_id, task_type, description",
            { rs, i -> response = AsyncTask(rs.getLong(1), rs.getString(2), objectMapper.readTree(rs.getString(3))) })
        return response
    }

    /**
     * Deletes task from database
     * @param taskId taskId in database
     * @return number of deleted tasks
     */
    fun deleteTask(taskId: Long): Int {
        return jdbcTemplate.update("DELETE FROM async_task WHERE task_id = ?", taskId)
    }

    /**
     * Delete previous equivalent tasks
     * @param lastTaskId newest taskId in database
     * @param taskType task type
     * @param property JSON property im description column we check for task equivalence
     * @param value expected value in [property] that corresponds to the same task
     * @return number of delete rows
     */
    fun deleteDuplicateTask(lastTaskId: Long, taskType: String, property: String, value: Long): Int {
        val deleted = jdbcTemplate.update(
            "DELETE FROM async_task WHERE " +
                    " task_id < ? AND task_type = ? AND CAST(description ->> ? AS BIGINT)  = ? ",
            lastTaskId, taskType, property, value
        )
        if (deleted > 0) {
            logger.info("Deleted $deleted tasks of taskType $taskType with $property = $value")
        }
        return deleted
    }

    fun tasks(limit: Int): List<AsyncTaskDto> {
        return jdbcTemplate.query(
            "SELECT task_id, task_type, description, attempt, process_time, taken FROM async_task LIMIT ?",
            { rs, i ->
                AsyncTaskDto(
                    rs.getLong(1), rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4), rs.getString(5), rs.getString(6)
                )
            },
            limit
        )
    }

    fun processNow(taskId: Long, expectedAttempt: Int): Int {
        return jdbcTemplate.update(
            "UPDATE async_task SET process_time = NOW() WHERE task_id = ? and attempt = ?", taskId, expectedAttempt
        )
    }
}
