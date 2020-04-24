package com.autonomousapps.advice

/**
 * Advice about dependencies.
 */
data class Advice(
  /**
   * The dependency that ought to be modified in some way.
   */
  val dependency: Dependency,
  /**
   * If this is "remove" advice, this _may_ be a non-empty set of transitive dependencies that are
   * used. This would indicate that this dependency is not safe to remove unless you are also adding
   * all the undeclared transitive dependencies.
   */
  val usedTransitiveDependencies: Set<Dependency> = emptySet(),
  /**
   * If this is "add" advice, then by definition this dependency has at least one "parent" that is
   * contributing it to the graph. In principle it may have many parents; hence this is a set.
   */
  val parents: Set<Dependency>? = null,
  /**
   * The current configuration on which the dependency has been declared. Will be null for
   * transitive dependencies.
   */
  val fromConfiguration: String? = null,
  /**
   * The configuration on which the dependency _should_ be declared. Will be null if the dependency
   * is unused and therefore ought to be removed.
   */
  val toConfiguration: String? = null
) : Comparable<Advice> {

  override fun compareTo(other: Advice): Int {
    return dependency.compareTo(other.dependency)
  }

  companion object {
    fun add(transitiveDependency: TransitiveDependency, toConfiguration: String) = Advice(
      dependency = transitiveDependency.dependency,
      parents = transitiveDependency.parents,
      fromConfiguration = null,
      toConfiguration = toConfiguration
    )

    fun remove(dependency: Dependency) = Advice(
      dependency = dependency,
      fromConfiguration = dependency.configurationName, toConfiguration = null
    )

    fun remove(component: ComponentWithTransitives) = Advice(
      dependency = component.dependency,
      usedTransitiveDependencies = component.usedTransitiveDependencies,
      fromConfiguration = component.dependency.configurationName,
      toConfiguration = null
    )

    fun change(dependency: Dependency, toConfiguration: String) = Advice(
      dependency = dependency,
      fromConfiguration = dependency.configurationName, toConfiguration = toConfiguration
    )

    fun compileOnly(dependency: Dependency, toConfiguration: String) = Advice(
      dependency = dependency,
      fromConfiguration = dependency.configurationName, toConfiguration = toConfiguration
    )
  }

  /**
   * `compileOnly` dependencies are special. If they are so declared, we assume the user knows what
   * they're doing and do not recommend changing them. We also don't recommend _adding_ a
   * compileOnly dependency that is only included transitively (to be less annoying).
   *
   * So, an advice is "compileOnly-advice" only if it is a compileOnly candidate and is declared on
   * a different configuration.
   */
  fun isCompileOnly() = toConfiguration?.endsWith("compileOnly", ignoreCase = true) == true

  /**
   * An advice is "add-advice" if it is undeclared and used, AND is not `compileOnly`.
   */
  fun isAdd() = fromConfiguration == null && !isCompileOnly()

  /**
   * An advice is "remove-advice" if it is declared and not used, AND is not `compileOnly`.
   */
  fun isRemove() = toConfiguration == null && !isCompileOnly()

  /**
   * An advice is "change-advice" if it is declared and used (but is on the wrong configuration),
   * AND is not `compileOnly`.
   */
  fun isChange() = fromConfiguration != null && toConfiguration != null && !isCompileOnly()
}
