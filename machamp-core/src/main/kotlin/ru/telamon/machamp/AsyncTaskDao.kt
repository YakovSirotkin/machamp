package ru.telamon.machamp

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import java.sql.Statement

/**
 * Database layer
 */
class AsyncTaskDao(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    /**
     * Creates task in database.
     * @param taskType type of the task
     * @param description data required for task processing
     * @return taskId in database
     */
    fun createTask(taskType: String, description: String): Long {
        val generatedKeyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({
            val ps = it.prepareStatement("INSERT INTO async_task (task_type, description) VALUES (?, ?::json)",
                    Statement.RETURN_GENERATED_KEYS)
            ps.setString(1, taskType)
            ps.setString(2, description)
            ps
        }, generatedKeyHolder)
        val taskId =  generatedKeyHolder.keys!!["task_id"] as Long
        logger.info("Created task $taskId of type $taskType with description $description")
        return taskId
    }

    /**
     * Takes one task with `process_time` in the past from database and moves its `process_time` to the future.
     * @return one [AsyncTask] or null
     */
    fun getTask(): AsyncTask? {
        var response: AsyncTask? = null
        jdbcTemplate.query("UPDATE async_task " +
                " SET process_time = NOW() + power(2, LEAST(14, attempt)) * interval '1 minute', " +
                " attempt = LEAST(30000, attempt + 1), " +
                " taken = NOW() WHERE task_id = " +
                " (SELECT task_id FROM async_task WHERE process_time < NOW()" +
                " LIMIT 1 FOR UPDATE SKIP LOCKED)" +
                " RETURNING task_id, task_type, description",
                { rs, i -> response = AsyncTask(rs.getLong(1), rs.getString(2), objectMapper.readTree(rs.getString(3))) })
        return response
    }

    /**
     * Deletes task from database
     * @param taskId taskId i database
     */
    fun deleteTask(taskId: Long) {
        jdbcTemplate.update("DELETE FROM async_task WHERE task_id = ?", taskId)
    }
}
