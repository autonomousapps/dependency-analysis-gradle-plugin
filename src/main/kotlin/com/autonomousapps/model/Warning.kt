package com.autonomousapps.model

import com.squareup.moshi.JsonClass

/** Warnings about the project for which the plugin cannot yet make universally actionable advice. */
@JsonClass(generateAdapter = false)
data class Warning(val duplicateClasses: Set<DuplicateClass>) {

  internal companion object {
    @JvmStatic
    fun empty() = Warning(emptySet())
  }

  fun isEmpty(): Boolean = duplicateClasses.isEmpty()
  fun isNotEmpty(): Boolean = !isEmpty()
}
