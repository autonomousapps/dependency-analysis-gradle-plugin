package com.autonomousapps.advice

import com.autonomousapps.internal.utils.asString
import com.autonomousapps.internal.utils.resolvedVersion
import org.gradle.api.artifacts.component.ComponentIdentifier

interface HasDependency {
  val dependency: Dependency
}

/**
 * Basically a tuple of [identifier] and [resolvedVersion] (and optionally the [configurationName]
 * on which this dependency is declared). `resolvedVersion` will be null for project dependencies,
 * and `configurationName` will be null for (at least) transitive dependencies.
 *
 * For equality purposes, this class only cares about its `identifier`. No other property matters.
 */
data class Dependency(
  /**
   * In group:artifact form. E.g.,
   * 1. "javax.inject:javax.inject"
   * 2. ":my-project"
   */
  val identifier: String,
  /**
   * Resolved version. Will be null for project dependencies.
   */
  val resolvedVersion: String? = null,
  /**
   * The configuration on which this dependency was declared, or null if none found.
   */
  val configurationName: String? = null
) : HasDependency, Comparable<Dependency> {

  internal constructor(componentIdentifier: ComponentIdentifier) : this(
    identifier = componentIdentifier.asString(),
    resolvedVersion = componentIdentifier.resolvedVersion()
  )

  override val dependency: Dependency = this

  /*
   * These overrides all basically say that we don't care about the resolved version for our algorithms. End-users
   * might care, which is why we include it anyway.
   */

  override fun compareTo(other: Dependency): Int = identifier.compareTo(other.identifier)

  override fun toString(): String {
    return if (resolvedVersion != null) {
      "$identifier:$resolvedVersion"
    } else {
      identifier
    }
  }

  /**
   * We only care about the [identifier] for equality comparisons.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Dependency

    if (identifier != other.identifier) return false

    return true
  }

  override fun hashCode(): Int = identifier.hashCode()
}