package com.autonomousapps.internal

internal fun getMetricsText(projectMetrics: ProjectMetrics): String {
  val origNodeCount = projectMetrics.origGraph.nodeCount
  val origEdgeCount = projectMetrics.origGraph.edgeCount
  val newNodeCount = projectMetrics.newGraph.nodeCount
  val newEdgeCount = projectMetrics.newGraph.edgeCount

  return "Current graph has $origNodeCount nodes and $origEdgeCount edges. If you follow all of " +
    "this advice, the new graph will have $newNodeCount nodes and $newEdgeCount edges.\n"
}

internal fun getMetricsText(buildMetrics: Map<String, ProjectMetrics>): String {
  var origNodesCount = 0f
  var origEdgesCount = 0f
  var newNodesCount = 0f
  var newEdgesCount = 0f
  
  buildMetrics.forEach { (_, metrics) ->
    origNodesCount += metrics.origGraph.nodeCount
    origEdgesCount += metrics.origGraph.edgeCount
    newNodesCount += metrics.newGraph.nodeCount
    newEdgesCount += metrics.newGraph.edgeCount
  }

  val origNodesAvg = origNodesCount / buildMetrics.size
  val origEdgesAvg = origEdgesCount / buildMetrics.size
  val newNodesAvg = newNodesCount / buildMetrics.size
  val newEdgesAvg = newEdgesCount / buildMetrics.size

  return "This build is composed of ${buildMetrics.size} individual projects, with an average " +
    "complexity of $origNodesAvg nodes and $origEdgesAvg edges. If you follow all of this advice, " +
    "the resultant complexity will average $newNodesAvg nodes and $newEdgesAvg edges.\n"
}
