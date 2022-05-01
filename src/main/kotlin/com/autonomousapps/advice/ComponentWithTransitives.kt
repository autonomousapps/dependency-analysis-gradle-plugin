package com.autonomousapps.advice

/**
 * Represents a dependency that is declared in the `dependencies {}` block of a build script. This
 * dependency may or may not be used, and has zero or more transitive dependencies that _are_ used
 * ([usedTransitiveDependencies]).
 */
@Deprecated(message = "To be deleted")
data class ComponentWithTransitives(
  /**
   * A tuple of an `identifier` and a resolved version. See [Dependency].
   */
  override val dependency: Dependency,
  /**
   * If this direct dependency has any transitive dependencies that are used, they will be in this
   * set. Will be null rather than empty.
   *
   * In group:artifact form. E.g.,
   * 1. "javax.inject:javax.inject"
   * 2. ":my-project"
   */
  val usedTransitiveDependencies: MutableSet<Dependency>?
) : HasDependency, Comparable<ComponentWithTransitives> {

  val identifier: String = dependency.identifier

  override fun compareTo(other: ComponentWithTransitives) = dependency.compareTo(other.dependency)
}
