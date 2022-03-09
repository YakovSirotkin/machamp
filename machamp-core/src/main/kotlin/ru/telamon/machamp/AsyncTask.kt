package ru.telamon.machamp

import com.fasterxml.jackson.databind.JsonNode

/**
 * Data structure for async tasks.
 */
data class AsyncTask(
    val taskId: Long,
    val taskType: String,
    val description: JsonNode
)
