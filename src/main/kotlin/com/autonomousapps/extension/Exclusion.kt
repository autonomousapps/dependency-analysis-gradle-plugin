package com.autonomousapps.extension

import java.io.Serializable

public sealed interface Exclusion: Serializable {
  public fun matches(name: String): Boolean

  public data class ExactMatch(val name: String): Exclusion {
    override fun matches(name: String): Boolean = this.name == name
  }

  public data class PatternMatch(val pattern: Regex): Exclusion {
    override fun matches(name: String): Boolean = name.matches(pattern)
  }
}

internal fun Collection<Exclusion>.anyMatches(name: String): Boolean = this.any { it.matches(name) }
