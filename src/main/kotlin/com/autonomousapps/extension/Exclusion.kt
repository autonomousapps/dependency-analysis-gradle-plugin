package com.autonomousapps.extension

import java.io.Serializable

sealed interface Exclusion: Serializable {
  fun matches(name: String): Boolean

  data class ExactMatch(val name: String): Exclusion {
    override fun matches(name: String) = this.name == name
  }

  data class PatternMatch(val pattern: Regex): Exclusion {
    override fun matches(name: String) = name.matches(pattern)
  }
}

internal fun Collection<Exclusion>.anyMatches(name: String): Boolean = this.any { it.matches(name) }
