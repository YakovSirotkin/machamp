package io.github.yakovsirotkin.machamp

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Core class that takes tasks from database and process it with [AsyncTaskHandler] implementations.
 */
@Component
class AsyncTaskProcessor(
    private val asyncTaskDao: AsyncTaskDao,
    @Value("\${machamp.processor.threads:10}")
    private val threadsCount: Int,
    @Value("\${machamp.processor.useCoroutines:false}")
    private val useCoroutines: Boolean,
    private val taskHandlers: List<AsyncTaskHandler>
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private lateinit var jobs: Array<Job>
    private lateinit var threadPool: ScheduledExecutorService
    private val taskHandlersMap = HashMap<String, AsyncTaskHandler>()

    private var continueProcessing: Boolean = true

    @PostConstruct
    fun init() {
        logger.info("Starting async task listener")

        taskHandlers.forEach {
            taskHandlersMap[it.getType()] = it
        }

        if (useCoroutines) {
            jobs = Array(threadsCount) { i ->
                logger.info("Launching task processor $i")
                GlobalScope.launch {
                    logger.info("Starting task processor coroutine $i")
                    while (continueProcessing) {
                        try {
                            processingCore(i)
                        } catch (e: Exception) {
                            logger.error("Error in coroutine processor $i", e)
                        }
                        delay(1000)
                    }
                    logger.info("Job processor coroutine $i will not process more tasks!")
                }
            }
        } else {
            threadPool = Executors.newScheduledThreadPool(threadsCount)
            for (i in 1..threadsCount) {
                threadPool.scheduleWithFixedDelay({
                    processingCore(i)
                }, 0, 1, TimeUnit.SECONDS)
            }
        }
    }

    /**
     * Normal processing without delays and exceptions.
     */
    private fun processingCore(processorId: Int) {
        try {
            while (continueProcessing) {
                val taskLoadStart = System.currentTimeMillis()
                logger.debug("Getting tasks for task processor $processorId")
                val task = asyncTaskDao.getTask()
                if (task != null) {
                    logger.info(
                        "Task $task loaded for {} ms by processor $processorId",
                        System.currentTimeMillis() - taskLoadStart
                    )

                    val taskType = task.taskType
                    val taskHandler = taskHandlersMap[taskType]
                    if (taskHandler == null) {
                        logger.error("Missing task handler for task type $taskType in processor $processorId")
                    } else {
                        logger.info("Start processing task $task by processor $processorId")
                        val processStart = System.currentTimeMillis()
                        try {
                            val needToDelete = taskHandler.process(task)
                            logger.info(
                                "Task $task processed for {} ms, needToDelete = $needToDelete by processor $processorId",
                                System.currentTimeMillis() - processStart
                            )
                            if (needToDelete) {
                                logger.info("Start deleting task $task in processor $processorId")
                                val deleteStart = System.currentTimeMillis()
                                asyncTaskDao.deleteTask(task.taskId)
                                logger.info(
                                    "Task $task deleted for {} ms by processor $processorId",
                                    System.currentTimeMillis() - deleteStart
                                )
                            }
                            logger.info(
                                "Task $task processing completed for {} ms by processor $processorId",
                                System.currentTimeMillis() - taskLoadStart
                            )
                        } catch (e: Throwable) {
                            logger.error(
                                "Task $task processing failed in {} ms by processor $processorId",
                                System.currentTimeMillis() - processStart, e
                            )
                        }
                    }
                } else {
                    logger.debug("No tasks for task processor $processorId")
                    break
                }
            }
        } catch (e: Throwable) {
            logger.error("Error trying to process task in processor $processorId", e)
        }
    }

    @PreDestroy
    fun onDestroy() {
        continueProcessing = false
        logger.info("Shutting down task processing")
        if (useCoroutines) {
            runBlocking {
                joinAll(*jobs)
            }
        } else {
            threadPool.shutdown()
        }
        logger.info("Task processing stopped")
    }
}
