package com.autonomousapps.internal.utils

/**
 * Appends value to the given Appendable and simple `\n` line separator after it.
 *
 * Always using the same line separator on all systems to allow for reproducible outputs.
 *
 * Borrowed from `org.gradle.kotlin.dsl.support`.
 */
internal fun Appendable.appendReproducibleNewLine(value: CharSequence = ""): Appendable {
  assert('\r' !in value) {
    "Unexpected line ending in string."
  }
  return append(value).append("\n")
}
