package ru.telamon.machamp.conf

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

@Suppress("SpellCheckingInspection")
object MachampCoroutineScope : CoroutineScope {
    private val threadGroup: ThreadGroup = Thread.currentThread().threadGroup
    private const val threadName = "machamp-thread"
    private val threadCounter = AtomicInteger(0)
    private val availableProcessors = Runtime.getRuntime().availableProcessors()

    private val dispatcher = Executors.newFixedThreadPool(availableProcessors) { runnable ->
        Thread(threadGroup, runnable).apply {
            this.name = "$threadName-${threadCounter.getAndIncrement()}"
            this.isDaemon = true
        }
    }.asCoroutineDispatcher()

    override val coroutineContext: CoroutineContext
        get() = dispatcher
}