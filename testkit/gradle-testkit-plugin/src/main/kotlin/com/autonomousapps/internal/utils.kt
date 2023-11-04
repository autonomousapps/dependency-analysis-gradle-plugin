package com.autonomousapps.internal

import java.util.Locale

internal fun String.capitalizeSafely(locale: Locale = Locale.ROOT): String {
  if (isNotEmpty()) {
    val firstChar = this[0]
    if (firstChar.isLowerCase()) {
      return buildString {
        val titleChar = firstChar.titlecaseChar()
        if (titleChar != firstChar.uppercaseChar()) {
          append(titleChar)
        } else {
          append(this@capitalizeSafely.substring(0, 1).uppercase(locale))
        }
        append(this@capitalizeSafely.substring(1))
      }
    }
  }
  return this
}
