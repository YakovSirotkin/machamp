package ru.telamon.machamp.conf

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import ru.telamon.machamp.AsyncTaskDao
import java.util.function.Supplier


internal class MachampAutoConfigurationTest {

    @Test
    fun autoConfigurationAvailable() {
        // given
        val context = AnnotationConfigApplicationContext()
        context.registerBean(JdbcTemplate::class.java, Supplier { Mockito.mock(JdbcTemplate::class.java) })
        context.registerBean(ObjectMapper::class.java, ObjectMapper())

        // when
        context.register(MachampAutoConfiguration::class.java)
        context.refresh()

        // then
        Assertions.assertDoesNotThrow {
            context.getBean(AsyncTaskDao::class.java)
        }
        context.close()
    }

    @Test
    fun autoConfigurationNotAvailableByDisabling() {
        // given
        val context = AnnotationConfigApplicationContext()
        TestPropertyValues.of("machamp.enabled=false").applyTo(context)

        // when
        context.register(MachampAutoConfiguration::class.java)
        context.refresh()

        // then
        Assertions.assertThrows(NoSuchBeanDefinitionException::class.java) {
            context.getBean(AsyncTaskDao::class.java)
        }
        context.close()
    }
}