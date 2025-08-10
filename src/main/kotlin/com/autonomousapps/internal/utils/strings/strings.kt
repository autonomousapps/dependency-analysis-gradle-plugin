// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils.strings

/**
 * Replaces every instance of [oldValue] with [newValue], except the last one.
 *
 * Much of this implementation was borrowed from `StringsJVM`'s [String.replace].
 */
internal fun String.replaceExceptLast(oldValue: String, newValue: String, ignoreCase: Boolean = false): String {
  val lastIndex = lastIndexOf(oldValue, ignoreCase = ignoreCase)

  if (lastIndex == -1) return this

  var occurrenceIndex: Int = indexOf(oldValue, 0, ignoreCase)
  // FAST PATH: no match
  if (occurrenceIndex < 0) return this

  val oldValueLength = oldValue.length
  val searchStep = oldValueLength.coerceAtLeast(1)
  val newLengthHint = length - oldValueLength + newValue.length
  if (newLengthHint < 0) throw OutOfMemoryError()
  val stringBuilder = StringBuilder(newLengthHint)

  var i = 0
  do {
    stringBuilder.append(this, i, occurrenceIndex).append(newValue)
    i = occurrenceIndex + oldValueLength
    if (occurrenceIndex >= length) break
    occurrenceIndex = indexOf(oldValue, occurrenceIndex + searchStep, ignoreCase)
  } while (occurrenceIndex > 0 && occurrenceIndex != lastIndex)

  return stringBuilder.append(this, i, length).toString()
}
