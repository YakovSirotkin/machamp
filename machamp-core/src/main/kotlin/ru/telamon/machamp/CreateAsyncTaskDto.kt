package ru.telamon.machamp

/**
 * Data structure for creating tasks from admin interface.
 */
data class CreateAsyncTaskDto(
    val taskType: String,
    val description: String
)