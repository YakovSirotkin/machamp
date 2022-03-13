package ru.telamon.machamp

/**
 * Interface that should be implemented to process specific task.
 */
interface AsyncTaskHandler {
    /**
     * @return type of tasks that this handler processes
     */
    fun getType(): String

    /**
     * Processing of the task.
     * @param asyncTask [AsyncTask] from database
     * @return 'true' if task was completed successfully and can be deleted, 'false' otherwise
     */
    fun process(asyncTask: AsyncTask): Boolean
}