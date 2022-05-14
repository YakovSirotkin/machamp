package io.github.yakovsirotkin.machamp

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@SpringBootTest(
    properties = [
        "machamp.priority.enabled=false"
    ]
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SumTest @Autowired constructor(
    private val sumAsyncTaskHandler: SumAsyncTaskHandler,
    private val asyncTaskDao: AsyncTaskDao,
    jdbcTemplate: JdbcTemplate
) :
    BaseTest(jdbcTemplate) {

    @Test
    fun sumTest() {
        runBlocking {
            val threads = 10
            val n = 100L
            for (mod in 0 until threads) {
                launch {
                    for (i in 0 until n) {
                        delay(10)
                        asyncTaskDao.createTask(
                            sumAsyncTaskHandler.getType(),
                            "{\"value\": ${i * threads + mod}}"
                        )
                    }
                }
            }
            val targetSum = (n * threads) * (n * threads - 1) / 2
            do {
                delay(1000)
                println(sumAsyncTaskHandler.getSum().toString() + " from " + targetSum)
            } while (sumAsyncTaskHandler.getSum() != targetSum)
        }
    }
}

@Component
open class SumAsyncTaskHandler : AsyncTaskHandler {

    private val sum = AtomicLong(0)

    private val random = Random(System.currentTimeMillis())

    override fun getType(): String {
        return "sum"
    }

    override fun process(asyncTask: AsyncTask): Boolean {
        if (random.nextDouble() < 0.01) {
            return false
        }
        sum.getAndAdd(asyncTask.description.get("value").asLong())
        return true
    }

    fun getSum(): Long {
        return sum.get()
    }
}