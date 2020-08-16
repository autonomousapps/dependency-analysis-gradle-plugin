package com.autonomousapps.graph

/**
 * An edge [from] &rarr; [to].
 */
internal data class Edge(
  val from: Node,
  val to: Node,
  val weight: Int = 1
) {
  fun nodeIds(): Pair<String, String> = from.identifier to to.identifier
}
