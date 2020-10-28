package com.autonomousapps.internal.utils

internal const val NORMAL = "\u001B[0m"
internal const val GREEN_BOLD = "\u001B[1;32m"

/**
 * Colorizes string for console output. Default is [GREEN_BOLD].
 *
 * See also
 * 1. [https://jakewharton.com/peeking-at-colorful-command-line-output/]
 * 2. [https://en.wikipedia.org/wiki/ANSI_escape_code]
 */
internal fun String.colorize(style: String = GREEN_BOLD): String {
  return "$style$this$NORMAL"
}
