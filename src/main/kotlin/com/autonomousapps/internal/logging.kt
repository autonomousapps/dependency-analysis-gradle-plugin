package com.autonomousapps.internal

import org.gradle.api.logging.Logger

private const val LOGGING = "logging"
private const val LOG_LEVEL_DEBUG = "debug"
private const val LOG_LEVEL_WARN = "warn"
private const val LOG_LEVEL_QUIET = "quiet"

internal fun Logger.log(msg: String) {
    when (System.getProperty(LOGGING, LOG_LEVEL_DEBUG)) {
        LOG_LEVEL_DEBUG -> debug(msg)
        LOG_LEVEL_WARN -> warn(msg)
        LOG_LEVEL_QUIET -> quiet(msg)
    }
}