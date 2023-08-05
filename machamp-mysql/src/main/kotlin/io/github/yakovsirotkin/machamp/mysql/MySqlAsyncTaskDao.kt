package io.github.yakovsirotkin.machamp.mysql

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.yakovsirotkin.machamp.AsyncTask
import io.github.yakovsirotkin.machamp.AsyncTaskDao
import io.github.yakovsirotkin.machamp.AsyncTaskDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.sql.ResultSet
import java.sql.Statement

@Component
@Primary
class MySqlAsyncTaskDao @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${machamp.priority.enabled:true}")
    private val priorityEnabled: Boolean,
    @Value("\${machamp.priority.defaultValue:100}")
    private val priorityDefaultValue: Int,
    @Value("\${machamp.taskTable:async_task}")
    private val taskTable: String,
) : AsyncTaskDao(jdbcTemplate, objectMapper, priorityEnabled, priorityDefaultValue, taskTable) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun createTask(
        taskType: String, description: String,
        priority: Int,
        delayInSeconds: Int
    ): Long {
        val generatedKeyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({
            val ps = it.prepareStatement(
                "INSERT INTO $taskTable (task_type, description, priority, process_time) " +
                        " VALUES (?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP , INTERVAL ? SECOND))",
                Statement.RETURN_GENERATED_KEYS
            )
            ps.setString(1, taskType)
            ps.setString(2, description)
            ps.setInt(3, priority)
            ps.setInt(4, delayInSeconds)
            ps
        }, generatedKeyHolder)
        val taskId = (generatedKeyHolder.keys!!["GENERATED_KEY"] as BigInteger).toLong()
        logger.info("Created task $taskId of type $taskType with description $description")
        return taskId
    }

    override fun getTask(): AsyncTask? {
        val orderClause = if (priorityEnabled) {
            "ORDER BY priority ASC"
        } else {
            ""
        }
        val asyncTasks = jdbcTemplate.query(
            """SELECT task_id, task_type, description, attempt, priority 
                FROM $taskTable
                WHERE process_time <= CURRENT_TIMESTAMP 
                $orderClause LIMIT ?""", { rs: ResultSet, i: Int ->
                AsyncTask(rs.getLong(1), rs.getString(2), objectMapper.readTree(rs.getString(3)),
                    rs.getInt(4), rs.getInt(5))

            }, 10
        )
        if (asyncTasks.isEmpty()) {
            return null
        }

        val topAsyncTask = if (priorityEnabled) {
            val topPriority = asyncTasks.first().priority
            asyncTasks.filter { it.priority == topPriority }
        } else {
            asyncTasks
        }.shuffled()

        topAsyncTask.forEach {
            val attempt = it.attempt
            val updatedRows = jdbcTemplate.update("""UPDATE $taskTable SET 
            process_time = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL POWER(2, LEAST(attempt, 14)) MINUTE),
            attempt = ?,
            taken = current_timestamp
            WHERE task_id = ? AND attempt = ?""",
            if (attempt < 30000) {
                attempt + 1
            } else {
                1000//Restart attempt count
            },
            it.taskId, attempt)

            if (updatedRows > 0) {
                logger.info("Got task $it with attempt = $attempt")
                return it
            } else {
                logger.info("Task ${it.taskId} was taken by other processor")
            }
        }
        return null
    }

    override fun deleteDuplicateTask(
        lastTaskId: Long,
        taskType: String,
        property: String,
        value: Long,
        priority: Int
    ): Int {
        val deleted = jdbcTemplate.update(
            "DELETE FROM $taskTable WHERE " +
                    " task_id < ? AND task_type = ? AND JSON_VALUE(description, '$.${property}') = ? " +
                    " AND priority >= ?",
            lastTaskId, taskType, value, priority
        )
        if (deleted > 0) {
            logger.info("Deleted $deleted tasks of taskType $taskType with $property = $value and priority >= $priority")
        }
        return deleted
    }

    override fun tasks(limit: Int): List<AsyncTaskDto> {
        return jdbcTemplate.query(
            "SELECT task_id, task_type, description, attempt, process_time, taken FROM $taskTable " +
                    " ORDER BY task_id LIMIT ? ",
            { rs, i ->
                AsyncTaskDto(
                    rs.getLong(1), rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4), rs.getString(5), rs.getString(6)
                )
            }, limit
        )
    }

    override fun processNow(taskId: Long, expectedAttempt: Int): Int {
        return jdbcTemplate.update(
            "UPDATE $taskTable SET process_time = CURRENT_TIMESTAMP WHERE task_id = ? and attempt = ?",
            taskId,
            expectedAttempt
        )
    }
}
