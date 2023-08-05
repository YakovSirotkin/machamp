package io.github.yakovsirotkin.machamp.client.mysql.springboot

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Machamp client configuration properties
 */
@ConfigurationProperties(prefix = "machamp")
open class ClientMachampProperties {
    val priority = Priority()
    var taskTable = "async_task"
    class Priority {
        var defaultValue: Int = 100
    }
}
