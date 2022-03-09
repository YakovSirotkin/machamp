package ru.telamon.machamp

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.telamon.machamp.conf.MachampCoroutineScope
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.system.measureTimeMillis

/**
 * Core class that takes tasks from database and process it with [AsyncTaskHandler] implementations.
 */
@Component
class AsyncTaskProcessor(
    private val asyncTaskDao: AsyncTaskDao,
    @Value("\${machamp.processor.threads:10}")
    private val threadsCount: Int,
    private val taskHandlers: List<AsyncTaskHandler>
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private lateinit var jobs: List<Job>
    private val taskHandlersMap = HashMap<String, AsyncTaskHandler>()

    private var continueProcessing: Boolean = true

    @PostConstruct
    fun init() {
        logger.info("Starting async task listener")

        taskHandlers.forEach {
            taskHandlersMap[it.getType()] = it
        }

        jobs = List(threadsCount) { i ->
            logger.info("Launching task processor $i")
            MachampCoroutineScope.launch {
                logger.info("Starting task processor $i")
                while (continueProcessing) {
                    try {
                        val taskLoadStart = System.currentTimeMillis()
                        logger.debug("Getting tasks for task processor $i")
                        val task = asyncTaskDao.getTask()
                        if (task != null) {
                            logger.info(
                                "Task $task loaded for {} ms by processor $i",
                                System.currentTimeMillis() - taskLoadStart
                            )

                            val taskType = task.taskType
                            val taskHandler = taskHandlersMap[taskType]
                            if (taskHandler == null) {
                                logger.error("Missing task handler for task type $taskType")
                            } else {
                                logger.info("Start processing task $task")
                                val processStart = System.currentTimeMillis()
                                try {
                                    val needToDelete = taskHandler.process(task)
                                    logger.info(
                                        "Task $task processed for {} ms, needToDelete = $needToDelete by processor $i",
                                        System.currentTimeMillis() - processStart
                                    )
                                    if (needToDelete) {
                                        deleteTask(task, i)
                                    }
                                    logger.info(
                                        "Task $task processing completed for {} ms by processor $i",
                                        System.currentTimeMillis() - taskLoadStart
                                    )
                                } catch (e: Throwable) {
                                    logger.error(
                                        "Task $task processing failed in {} ms by processor $i",
                                        System.currentTimeMillis() - processStart, e
                                    )
                                }
                            }
                        } else {
                            logger.debug("No tasks for task processor $i")
                        }
                    } catch (e: Throwable) {
                        logger.error("Error trying to load process task in processor $i", e)
                    }
                    delay(1000)
                }
                logger.info("Job processor $i will not process more tasks!")
            }
        }
    }

    private fun deleteTask(task: AsyncTask, processorId: Int) {
        with(logger) {
            info("Start deleting task $task")
            val time = measureTimeMillis { asyncTaskDao.deleteTask(task.taskId) }
            info("Task $task deleted for {} ms by processor $processorId", time)
        }
    }

    @PreDestroy
    fun onDestroy() = runBlocking {
        continueProcessing = false
        logger.info("Shutting down task processing")
        jobs.joinAll()
        logger.info("Task processing stopped")
    }
}
