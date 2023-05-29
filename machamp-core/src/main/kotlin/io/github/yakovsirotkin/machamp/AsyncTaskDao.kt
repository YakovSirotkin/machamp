package io.github.yakovsirotkin.machamp

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import java.sql.Statement
import javax.annotation.PostConstruct

/**
 * Database layer
 */
@Component
@Qualifier
open class AsyncTaskDao @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${machamp.priority.enabled:true}")
    private val priorityEnabled: Boolean,
    @Value("\${machamp.priority.defaultValue:100}")
    private val priorityDefaultValue: Int,
    @Value("\${machamp.taskTable:async_task}")
    private val taskTable: String,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    @PostConstruct
    fun checkTableNameToPreventPossibleAttacks() {
        if (taskTable.length > 50) {
            throw Exception("Task table name $taskTable has more than 50 symbols")
        }
        " \t\r\n,;".forEach {
            if (taskTable.contains(it)) {
                throw Exception("Task table name $taskTable contains symbol $it (code ${it.code})")
            }
        }
        logger.info("Setting async_task table name to $taskTable")
    }

    /**
     * Creates task in database.
     * @param taskType type of the task
     * @param description data required for task processing
     * @param priority task priority, process tasks with less priority value earlier
     * @param delayInSeconds delay before first attempt in seconds
     * @return taskId in database
     */
    open fun createTask(
        taskType: String, description: String,
        priority: Int = priorityDefaultValue, delayInSeconds: Int = 0
    ): Long {
        val generatedKeyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({
            val ps = it.prepareStatement(
                "INSERT INTO $taskTable (task_type, description, priority, process_time) " +
                        " VALUES (?, ?::json, ?, NOW() + ? * INTERVAL '1' SECOND )",
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
    open fun getTask(): AsyncTask? {
        var response: AsyncTask? = null
        jdbcTemplate.query(
            "UPDATE $taskTable " +
                    " SET process_time = NOW() + power(2, LEAST(14, attempt)) * interval '1 minute', " +
                    " attempt = LEAST(30000, attempt + 1), " +
                    " taken = NOW() WHERE task_id = " +
                    " (SELECT task_id FROM $taskTable WHERE process_time < NOW()" +
                    if (priorityEnabled) {
                        " ORDER BY priority ASC "
                    } else {
                        ""
                    } +
                    " LIMIT 1 FOR UPDATE SKIP LOCKED)" +
                    " RETURNING task_id, task_type, description, attempt, priority "
        ) {
                rs, _ ->
            response = AsyncTask(rs.getLong(1), rs.getString(2), objectMapper.readTree(rs.getString(3)),
                    rs.getInt(4), rs.getInt(5))
        }
        return response
    }

    /**
     * Deletes task from database
     * @param taskId taskId in database
     * @return number of deleted tasks
     */
    open fun deleteTask(taskId: Long): Int {
        return jdbcTemplate.update("DELETE FROM $taskTable WHERE task_id = ?", taskId)
    }

    /**
     * Delete previous equivalent tasks
     * @param lastTaskId newest taskId in database
     * @param taskType task type
     * @param property JSON property im description column we check for task equivalence
     * @param value expected value in [property] that corresponds to the same task
     * @param priority only tasks with priority greater or equal will be removed
     * @return number of delete rows
     */
    open fun deleteDuplicateTask(
        lastTaskId: Long,
        taskType: String,
        property: String,
        value: Long,
        priority: Int = priorityDefaultValue
    ): Int {
        val deleted = jdbcTemplate.update(
            "DELETE FROM $taskTable WHERE " +
                    " task_id < ? AND task_type = ? AND CAST(description ->> ? AS BIGINT) = ? " +
                    " AND priority >= ?",
            lastTaskId, taskType, property, value, priority
        )
        if (deleted > 0) {
            logger.info("Deleted $deleted tasks of taskType $taskType with $property = $value and priority >= $priority")
        }
        return deleted
    }

    open fun tasks(limit: Int): List<AsyncTaskDto> {
        return jdbcTemplate.query(
            "SELECT task_id, task_type, description, attempt, process_time, taken FROM $taskTable LIMIT ?",
            { rs, _ ->
                AsyncTaskDto(
                    rs.getLong(1), rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4), rs.getString(5), rs.getString(6)
                )
            },
            limit
        )
    }

    open fun processNow(taskId: Long, expectedAttempt: Int): Int {
        return jdbcTemplate.update(
            "UPDATE $taskTable SET process_time = NOW() WHERE task_id = ? and attempt = ?", taskId, expectedAttempt
        )
    }
}
