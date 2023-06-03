package io.github.yakovsirotkin.machamp.client.sqlserver.springboot

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Machamp client configuration properties
 */
@ConfigurationProperties(prefix = "machamp")
open class OracleClientMachampProperties {
    val priority = Priority()
    var taskTable = "async_task"
    var taskSequence = "async_task_seq"

    class Priority {
        var defaultValue: Int = 100
    }
}