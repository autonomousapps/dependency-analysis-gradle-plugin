// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
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
   * @see <a href="https://jakewharton.com/peeking-at-colorful-command-line-output/">Peeking at command-line ANSI escape sequences</a>
   * @see <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape code</a>
   */
  internal fun String.colorize(style: String = GREEN_BOLD): String {
    // The complexity of this implementation is due to the fact that ANSI escape codes don't persist across newline
    // boundaries. So we need to wrap each line in the codes. We make a best effort to ensure the final, colorized
    // string retains the newline (or not) at the end of the original string.

    val lines = lines()
    val appendNewlineAtEnd = endsWith(System.lineSeparator())
    val lineCount = lines.size

    return buildString {
      lines.forEachIndexed { i, line ->
        append(style)
        append(line)
        if (i < lineCount - 1) {
          appendLine()
        }
        append(NORMAL)
      }

      if (appendNewlineAtEnd) {
        appendLine()
      }
    }
  }
}
