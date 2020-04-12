package com.autonomousapps.internal.utils

import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

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

internal inline fun <reified T> getLogger(): Logger = Logging.getLogger(T::class.java)

internal fun Task.chatter(isChatty: Boolean) = Chatter(logger, isChatty)

internal class Chatter(
  private val logger: Logger,
  private val isChatty: Boolean
) {

  fun chat(msg: String) {
    when (isChatty) {
      true -> logger.quiet(msg)
      false -> logger.info(msg)
    }
  }
}

