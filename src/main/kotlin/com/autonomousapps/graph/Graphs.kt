@file:Suppress("UnstableApiUsage") // Guava Graph API

package com.autonomousapps.graph

import com.autonomousapps.internal.utils.filterNotToSet
import com.autonomousapps.model.Coordinates
import com.google.common.graph.Graph

internal object Graphs {

  fun Graph<Coordinates>.adj(node: Coordinates): Set<Coordinates> = adjacentNodes(node)

  fun Graph<Coordinates>.reachableNodes(node: Coordinates): Set<Coordinates> {
    return com.google.common.graph.Graphs.reachableNodes(this, node)
      // exclude self from list
      .filterNotToSet { it == node }
  }

  fun Graph<Coordinates>.children(node: Coordinates): Set<Coordinates> = successors(node)
}
