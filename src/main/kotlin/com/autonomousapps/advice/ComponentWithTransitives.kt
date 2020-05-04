package com.autonomousapps.advice

/**
 * Represents a dependency ([Dependency.identifier]) that is declared in the `dependencies {}` block
 * of a build script. This dependency may or may not be used, and has zero or more transitive
 * dependencies that _are_ used ([usedTransitiveDependencies]).
 */
data class ComponentWithTransitives(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  override val dependency: Dependency,
  /**
   * If this direct dependency has any transitive dependencies that are used, they will be in this
   * set.
   *
   * In group:artifact form. E.g.,
   * 1. "javax.inject:javax.inject"
   * 2. ":my-project"
   */
  val usedTransitiveDependencies: MutableSet<Dependency>
) : HasDependency, Comparable<ComponentWithTransitives> {

  /**
   * A `ComponentWithTransitives` is a "facade" dependency if it has a "children" with the same
   * group (e.g., "com.something").
   */
  val isFacade: Boolean
    get() = dependency.group != null && usedTransitiveDependencies.any {
      it.group == dependency.group
    }

  override fun compareTo(other: ComponentWithTransitives): Int {
    return dependency.compareTo(other.dependency)
  }
}
