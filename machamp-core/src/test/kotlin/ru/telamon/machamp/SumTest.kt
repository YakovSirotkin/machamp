package ru.telamon.machamp

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@SpringBootTest
@Testcontainers
class SumTest @Autowired constructor(
    private var sumAsyncTaskHandler: SumAsyncTaskHandler,
    private var asyncTaskDao: AsyncTaskDao
): BaseTest() {

    companion object {
        @Container
        private val postgresDB: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:12")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("sql/001-init.sql")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresDB::getJdbcUrl)
            registry.add("spring.datasource.jdbcUrl", postgresDB::getJdbcUrl)
            registry.add("spring.datasource.username", postgresDB::getUsername)
            registry.add("spring.datasource.password", postgresDB::getPassword)
        }
    }

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
                println(sumAsyncTaskHandler.getSum().toString() + " from "  + targetSum)
            } while ( sumAsyncTaskHandler.getSum() != targetSum)
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

    fun getSum() : Long {
        return sum.get()
    }
}