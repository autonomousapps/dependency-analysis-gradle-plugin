package com.autonomousapps.advice

/**
 * A dependency that has not been declared, but which is included by one or more other dependencies,
 * transitively.
 */
data class TransitiveDependency(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  override val dependency: Dependency,
  /**
   * The "parent" dependencies that all contribute this transitive dependency.
   */
  val parents: Set<Dependency>
) : HasDependency, Comparable<TransitiveDependency> {

  /**
   * A `TransitiveDependency` is part of a "facade" dependency if it has a "parent" with the same
   * group (e.g., "com.something").
   */
  val isFacade: Boolean by lazy {
    dependency.group != null && parents.any { it.group == dependency.group }
  }

  override fun compareTo(other: TransitiveDependency): Int = dependency.compareTo(other.dependency)
}