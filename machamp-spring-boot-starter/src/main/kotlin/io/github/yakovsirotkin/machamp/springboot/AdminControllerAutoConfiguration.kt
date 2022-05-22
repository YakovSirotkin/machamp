package io.github.yakovsirotkin.machamp.springboot

import io.github.yakovsirotkin.machamp.AdminController
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * AutoConfigure
 */
@Configuration
@Import(AdminController::class)
open class AdminControllerAutoConfiguration