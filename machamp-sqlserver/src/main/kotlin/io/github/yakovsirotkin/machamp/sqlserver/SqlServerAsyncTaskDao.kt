package io.github.yakovsirotkin.machamp.sqlserver

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
import java.math.BigDecimal
import java.sql.Statement

@Component
@Primary
class SqlServerAsyncTaskDao @Autowired constructor(
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
                        " VALUES (?, ?, ?,  DATEADD(ss, ?, GETUTCDATE()))",
                Statement.RETURN_GENERATED_KEYS
            )
            ps.setString(1, taskType)
            ps.setString(2, description)
            ps.setInt(3, priority)
            ps.setInt(4, delayInSeconds)
            ps
        }, generatedKeyHolder)
        val taskId = (generatedKeyHolder.keys!!["GENERATED_KEYS"] as BigDecimal).longValueExact()
        logger.info("Created task $taskId of type $taskType with description $description")
        return taskId
    }

    override fun getTask(): AsyncTask? {
        var response: AsyncTask? = null
        jdbcTemplate.query(
            ";with cte as (" +
                    "SELECT TOP(1) " +
                    " task_id, task_type, description, process_time, attempt, taken  " +
                    " FROM $taskTable " +
                    " WHERE process_time < GETUTCDATE() " +
                    if (priorityEnabled) {
                        " ORDER BY priority ASC "
                    } else {
                        ""
                    } +
                    " ) " +
                    " UPDATE cte SET process_time = DATEADD(MINUTE, power(2, CASE WHEN 14 < attempt THEN 14 ELSE attempt END), GETUTCDATE()), " +
                    " attempt = CASE WHEN 30000 < attempt THEN 30000 ELSE attempt + 1 END, " +
                    " taken = GETUTCDATE() " +
                    " OUTPUT " +
                    " DELETED.task_id, DELETED.task_type, DELETED.description",
            { rs, i ->
                response = AsyncTask(rs.getLong(1), rs.getString(2), objectMapper.readTree(rs.getString(3)))
            })
        return response
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
                    " task_id < ? AND task_type = ? AND CAST(JSON_VALUE(description, ?) AS INT) = ? " +
                    " AND priority >= ?",
            lastTaskId, taskType, "$.$property", value, priority
        )
        if (deleted > 0) {
            logger.info("Deleted $deleted tasks of taskType $taskType with $property = $value and priority >= $priority")
        }
        return deleted
    }

    override fun tasks(limit: Int): List<AsyncTaskDto> {
        return jdbcTemplate.query(
            "SELECT TOP $limit task_id, task_type, description, attempt, process_time, taken FROM $taskTable " +
                    " ORDER BY task_id"
        ) { rs, i ->
            AsyncTaskDto(
                rs.getLong(1), rs.getString(2),
                rs.getString(3),
                rs.getInt(4), rs.getString(5), rs.getString(6)
            )
        }
    }

    override fun processNow(taskId: Long, expectedAttempt: Int): Int {
        return jdbcTemplate.update(
            "UPDATE $taskTable SET process_time = GETUTCDATE() WHERE task_id = ? and attempt = ?", taskId, expectedAttempt
        )
    }
}