package io.github.yakovsirotkin.machamp.springboot

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Machamp configuration properties
 */
@ConfigurationProperties(prefix = "machamp")
open class MachampProperties {
    val processor = Processor()
    val priority = Priority()
    var taskTable = "async_task"

    class Processor {
       var threads: Int = 10
       var useCoroutines: Boolean = false
    }

    class Priority {
        var enabled: Boolean = true
        var defaultValue: Int = 100
    }
}