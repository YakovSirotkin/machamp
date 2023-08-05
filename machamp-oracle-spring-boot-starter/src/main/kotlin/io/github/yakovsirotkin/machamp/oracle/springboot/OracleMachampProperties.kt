package io.github.yakovsirotkin.machamp.oracle.springboot

import io.github.yakovsirotkin.machamp.springboot.MachampProperties
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Machamp configuration properties for Oracle
 */
@ConfigurationProperties(prefix = "machamp")
open class OracleMachampProperties : MachampProperties() {
    var taskSequence = "async_task_seq"
}
