package ru.telamon.machamp.conf

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("machamp")
class MachampProperties {

    /**
     * Enable async processing.
     */
    var enabled = true

    /**
     * Parallel workers number.
     */
    var processorParallelismLevel = 10
}