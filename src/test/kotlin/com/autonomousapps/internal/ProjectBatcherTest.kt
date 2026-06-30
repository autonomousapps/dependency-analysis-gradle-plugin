package com.autonomousapps.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ProjectBatcherTest {

  @Test fun `batches projects into groups of specified size`() {
    val paths = (1..250).map { ":project$it" }.toSet()
    val batches = ProjectBatcher.batch(paths, batchSize = 100)
    assertEquals(3, batches.size)
    assertEquals(100, batches[0].size)
    assertEquals(100, batches[1].size)
    assertEquals(50, batches[2].size)
  }

  @Test fun `single batch when projects fewer than batch size`() {
    val paths = (1..50).map { ":project$it" }.toSet()
    val batches = ProjectBatcher.batch(paths, batchSize = 100)
    assertEquals(1, batches.size)
    assertEquals(50, batches[0].size)
  }

  @Test fun `all projects included across batches`() {
    val paths = (1..250).map { ":project$it" }.toSet()
    val batches = ProjectBatcher.batch(paths, batchSize = 100)
    val allFromBatches = batches.flatten().toSet()
    assertEquals(paths, allFromBatches)
  }

  @Test fun `batch size of zero or negative defaults to all-in-one`() {
    val paths = (1..250).map { ":project$it" }.toSet()
    val batches = ProjectBatcher.batch(paths, batchSize = 0)
    assertEquals(1, batches.size)
    assertEquals(250, batches[0].size)
  }
}
