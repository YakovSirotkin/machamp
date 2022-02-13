package ru.telamon.machamp

import com.fasterxml.jackson.databind.JsonNode

data class  AsyncTask(
    val taskId: Long,
    val taskType: String,
    val description: JsonNode
)
