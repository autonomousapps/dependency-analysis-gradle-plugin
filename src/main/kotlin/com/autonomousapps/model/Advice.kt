// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.internal.utils.isTrue
import com.autonomousapps.model.internal.declaration.Declaration
import com.squareup.moshi.JsonClass

/**
 * An "advice" is a kind of _transform_ that users ought to perform to bring their dependency declarations into a more
 * correct state.
 *
 * See also [Usage][com.autonomousapps.model.internal.intermediates.Usage].
 */
@JsonClass(generateAdapter = false)
public data class Advice(
  /** The coordinates of the dependency that ought to be modified in some way. */
  val coordinates: Coordinates,
  /** The current configuration on which the dependency has been declared. Will be null for transitive dependencies. */
  val fromConfiguration: String? = null,
  /**
   * The configuration on which the dependency _should_ be declared. Will be null if the dependency is unused and
   * therefore ought to be removed.
   */
  val toConfiguration: String? = null
) : Comparable<Advice> {

  override fun compareTo(other: Advice): Int = compareBy(Advice::coordinates)
    .thenComparing(compareBy<Advice, String?>(nullsFirst()) { it.toConfiguration })
    .thenComparing(compareBy<Advice, String?>(nullsFirst()) { it.fromConfiguration })
    .compare(this, other)

  public companion object {
    @JvmStatic
    public fun ofAdd(coordinates: Coordinates, toConfiguration: String): Advice = Advice(
      coordinates = coordinates,
      fromConfiguration = null,
      toConfiguration = toConfiguration
    )

    @JvmStatic
    public fun ofRemove(coordinates: Coordinates, fromConfiguration: String): Advice = Advice(
      coordinates = coordinates,
      fromConfiguration = fromConfiguration, toConfiguration = null
    )

    @JvmStatic
    internal fun ofRemove(coordinates: Coordinates, declaration: Declaration) =
      ofRemove(coordinates, declaration.configurationName)

    @JvmStatic
    public fun ofChange(coordinates: Coordinates, fromConfiguration: String, toConfiguration: String): Advice {
      require(fromConfiguration != toConfiguration) {
        "Change advice for ${coordinates.identifier} cannot be from and to the same configuration ($fromConfiguration in this case)"
      }

      return Advice(
        coordinates = coordinates,
        fromConfiguration = fromConfiguration,
        toConfiguration = toConfiguration
      )
    }
  }

  /**
   * `compileOnly` dependencies are special. If they are so declared, we assume the user knows what they're doing and do
   * not generally recommend changing them. We also don't recommend _adding_ a compileOnly dependency that is only
   * included transitively (to be less annoying).
   *
   * So, an advice is "compileOnly-advice" only if it is a compileOnly candidate and is declared on a different
   * configuration.
   */
  public fun isCompileOnly(): Boolean = toConfiguration?.endsWith("compileOnly", ignoreCase = true) == true

  public fun isRemoveCompileOnly(): Boolean = isRemove() && fromConfiguration?.endsWith("compileOnly", ignoreCase = true) == true

  public fun isRuntimeOnly(): Boolean = toConfiguration?.endsWith("runtimeOnly", ignoreCase = true) == true

  /**
   * An advice is "add-advice" if it is undeclared and used, AND is not `compileOnly`.
   */
  public fun isAdd(): Boolean = isAnyAdd() && !isCompileOnly()

  public fun isAnyAdd(): Boolean = fromConfiguration == null && toConfiguration != null

  /**
   * An advice is "remove-advice" if it is declared and not used, AND is not `compileOnly`,
   * AND is not `processor`.
   */
  public fun isRemove(): Boolean = isAnyRemove() && !isCompileOnly() && !isProcessor()

  public fun isAnyRemove(): Boolean = toConfiguration == null

  /**
   * An advice is "change-advice" if it is declared and used (but is on the wrong configuration),
   * AND is not `compileOnly`, AND is not `runtimeOnly`.
   */
  public fun isChange(): Boolean = isAnyChange() && !isCompileOnly() && !isRuntimeOnly()

  /**
   * An advice is "change-advice" if it is declared and used (but is on the wrong configuration).
   */
  public fun isAnyChange(): Boolean = fromConfiguration != null && toConfiguration != null

  /**
   * An advice is "processors-advice" if it is declared on a k/apt or annotationProcessor
   * configuration, and this dependency should be removed.
   */
  public fun isProcessor(): Boolean = toConfiguration == null && fromConfiguration?.let {
    it.endsWith("kapt", ignoreCase = true) || it.endsWith("annotationProcessor", ignoreCase = true)
  }.isTrue()

  /** If this is advice to remove or downgrade a dependency. */
  public fun isDowngrade(): Boolean = (isRemove() || isCompileOnly() || isRuntimeOnly())

  /** If this is advice to add a dependency, or change an existing dependency to make it api-like. */
  public fun isUpgrade(): Boolean = isAnyAdd() || (isAnyChange() && isToApiLike())

  public fun isToApiLike(): Boolean = toConfiguration?.endsWith("api", ignoreCase = true) == true
}
