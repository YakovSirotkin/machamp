package ru.telamon.machamp

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import java.sql.Statement

@Component
class AsyncTaskDao @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

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

    fun deleteTask(taskId: Long) {
        jdbcTemplate.update("DELETE FROM async_task WHERE task_id = ?", taskId)
    }
}
