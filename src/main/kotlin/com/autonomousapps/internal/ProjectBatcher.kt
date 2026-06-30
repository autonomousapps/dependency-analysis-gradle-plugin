package com.autonomousapps.internal

internal object ProjectBatcher {
  fun batch(projectPaths: Set<String>, batchSize: Int): List<List<String>> {
    if (batchSize <= 0) return listOf(projectPaths.toList())
    return projectPaths.toList().sorted().chunked(batchSize)
  }
}
