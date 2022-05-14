package io.github.yakovsirotkin.machamp

/**
 * Data structure with full info about AsyncTask for admin interface
 */
data class AsyncTaskDto(
    val taskId: Long,
    val taskType: String,
    val description: String,
    val attempt: Int,
    val processTime: String,
    val taken: String?
)
