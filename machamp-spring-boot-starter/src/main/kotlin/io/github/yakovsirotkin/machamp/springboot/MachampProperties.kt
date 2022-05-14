package io.github.yakovsirotkin.machamp.springboot

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Machamp configuration properties
 */
@ConfigurationProperties(prefix = "machamp")
class MachampProperties {
    val processor = Processor()
    val priority = Priority()

    class Processor {
       var threads: Int = 10
    }

    class Priority {
        var enabled: Boolean = true
    }
}