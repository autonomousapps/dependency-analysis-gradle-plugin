package com.autonomousapps.advice

import com.autonomousapps.internal.utils.toIdentifier
import com.autonomousapps.internal.utils.resolvedVersion
import org.gradle.api.artifacts.component.ComponentIdentifier

@Deprecated(message = "To be deleted")
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
@Deprecated(message = "To be deleted")
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
    identifier = componentIdentifier.toIdentifier(),
    resolvedVersion = componentIdentifier.resolvedVersion()
  )

  init {
    check(resolvedVersion != "") {
      "Version string must not be empty. Use null instead."
    }
  }

  override val dependency: Dependency = this

  /**
   * The artifact's group. Project dependencies have no group.
   */
  val group: String? = computeGroup()

  private fun computeGroup(): String? {
    if (identifier.startsWith(":")) return null

    val index = identifier.indexOf(':')
    return if (index != -1) {
      identifier.substring(0, index).intern()
    } else {
      null
    }
  }

  /*
   * We only care about the identifier and the resolvedVersion.
   */

  override fun toString(): String =
    if (resolvedVersion != null) "$identifier:$resolvedVersion"
    else identifier

  /**
   * We only care about the [identifier] and [resolvedVersion] for comparisons.
   *
   * nb: "a" > ":" implies external > internal.
   */
  override fun compareTo(other: Dependency): Int {
    val byIdentifier = identifier.compareTo(other.identifier)
    if (byIdentifier != 0) return byIdentifier

    // identifiers are equal
    if (resolvedVersion == null && other.resolvedVersion == null) return 0
    if (other.resolvedVersion == null) return 1
    return resolvedVersion!!.compareTo(other.resolvedVersion)
  }

  /**
   * We only care about the [identifier] and [resolvedVersion] for equality comparisons.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Dependency

    if (identifier != other.identifier) return false
    if (resolvedVersion != other.resolvedVersion) return false

    return true
  }

  /**
   * We only care about the [identifier] and [resolvedVersion] for hashing.
   */
  override fun hashCode(): Int {
    var result = identifier.hashCode()
    result = 31 * result + (resolvedVersion?.hashCode() ?: 0)
    return result
  }
}
