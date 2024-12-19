package com.autonomousapps.internal.graph.supers

import com.autonomousapps.model.Coordinates

/** A super class or interface in an inheritance/composition graph. */
internal data class SuperNode(
  val className: String,
) : Comparable<SuperNode> {

  override fun compareTo(other: SuperNode): Int {
    return className.compareTo(other.className)
  }

  /**
   * The dependencies that supply this [className]. May be more than one in the presence of classpath duplication. May
   * be empty if the [className] is from the JDK, like `java.lang.Object`.
   */
  val deps: MutableSet<Coordinates> = mutableSetOf()
}
