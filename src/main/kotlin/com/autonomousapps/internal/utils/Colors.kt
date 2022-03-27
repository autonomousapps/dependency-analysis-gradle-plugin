package com.autonomousapps.internal.utils

internal object Colors {
  internal const val NORMAL = "\u001B[0m"
  internal const val BOLD = "\u001B[1m"

  internal const val RED = "\u001B[31m"
  internal const val GREEN = "\u001B[32m"
  internal const val YELLOW = "\u001B[33m"

  internal const val RED_BOLD = "\u001B[1;31m"
  internal const val GREEN_BOLD = "\u001B[1;32m"
  internal const val YELLOW_BOLD = "\u001B[1;33m"

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
}
