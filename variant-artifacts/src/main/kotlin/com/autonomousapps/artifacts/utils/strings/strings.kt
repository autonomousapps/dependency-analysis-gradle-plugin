package com.autonomousapps.artifacts.utils.strings

private val isDelimiter: (Char) -> Boolean = { !Character.isLetterOrDigit(it) }

// TODO: move to utils module?
internal fun String.camelCase(): String {
  if (isEmpty()) return this

  var upperCaseNext = false

  return buildString {
    val chars = toCharArray()
    var i = 0
    var isFirstChar = true

    while (i in chars.indices) {
      val char = chars[i++]

      if (upperCaseNext) {
        if (isDelimiter(char)) {
          continue
        }
      }

      if (isDelimiter(char)) {
        upperCaseNext = true
        continue
      } else {
        if (Character.isLetterOrDigit(char)) {
          if (upperCaseNext && !isFirstChar) {
            append(char.uppercaseChar())
          } else {
            append(char.lowercaseChar())
          }

          isFirstChar = false
        }

        upperCaseNext = false
      }
    }
  }
}
