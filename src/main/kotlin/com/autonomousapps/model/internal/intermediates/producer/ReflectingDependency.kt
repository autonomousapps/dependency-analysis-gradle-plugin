// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.producer

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.Capability
import com.autonomousapps.model.internal.InferredCapability

/**
 * [accessor] has classes [accessingClasses] (1+) that uses [Class.forName] to reflectively access [accessedClass],
 * which lives in [coordinates].
 *
 * E.g.,
 * `com.foo:bar:1.0` has class `com.foo.Foo` that invokes `Class.forName("com.bar.Bar")`.
 */
internal data class ReflectingDependency(
  override val coordinates: Coordinates,
  val accessor: Coordinates,
  val accessingClasses: Set<String>,
  val accessedClass: String,
) : DependencyView<ReflectingDependency>, Comparable<ReflectingDependency> {

  data class ReflectiveAccess(
    val accessor: Coordinates,
    val accessingClasses: Set<String>,
    val accessedClass: String,
  ) : Comparable<ReflectiveAccess> {

    init {
      require(accessingClasses.isNotEmpty())
    }

    fun asReason(): String {
      return buildString {
        append(accessor.gav())
        append(" in class ")

        if (accessingClasses.size == 1) {
          append(accessingClasses.first())
        } else {
          append(accessingClasses)
          append("*")
        }

        append(": ")
        append(accessedClass)
      }
    }

    override fun compareTo(other: ReflectiveAccess): Int {
      return compareBy(ReflectiveAccess::accessor)
        .thenBy(LexicographicIterableComparator()) { it.accessingClasses }
        .thenBy(ReflectiveAccess::accessedClass)
        .compare(this, other)
    }
  }

  fun asReflectiveAccess() = ReflectiveAccess(accessor, accessingClasses, accessedClass)

  override fun compareTo(other: ReflectingDependency): Int {
    return compareBy(ReflectingDependency::coordinates)
      .thenBy(ReflectingDependency::accessor)
      .thenBy(LexicographicIterableComparator()) { it.accessingClasses }
      .thenBy(ReflectingDependency::accessedClass)
      .compare(this, other)
  }

  override fun toCapabilities(): List<Capability> {
    return listOf(InferredCapability(false, setOf(asReflectiveAccess())))
  }
}
