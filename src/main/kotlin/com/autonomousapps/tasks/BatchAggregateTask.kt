// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Collects per-project health reports for a batch of projects and copies them
 * into a batch output directory. This limits how many project outputs must be
 * held in memory simultaneously during final aggregation.
 */
@CacheableTask
public abstract class BatchAggregateTask : DefaultTask() {

  init {
    description = "Collects per-project analysis reports for a batch of projects"
  }

  @get:Input
  public abstract val batchIndex: Property<Int>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  public abstract val projectHealthReports: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  public abstract val projectMetadataReports: ConfigurableFileCollection

  @get:OutputDirectory
  public abstract val outputDir: DirectoryProperty

  @TaskAction
  public fun action() {
    val outDir = outputDir.get().asFile
    outDir.deleteRecursively()
    outDir.mkdirs()

    val healthDir = outDir.resolve("health")
    healthDir.mkdirs()
    projectHealthReports.files.forEachIndexed { idx, file ->
      if (file.exists()) {
        file.copyTo(healthDir.resolve("${idx}.json"), overwrite = true)
      }
    }

    val metadataDir = outDir.resolve("metadata")
    metadataDir.mkdirs()
    projectMetadataReports.files.forEachIndexed { idx, file ->
      if (file.exists()) {
        file.copyTo(metadataDir.resolve("${idx}.json"), overwrite = true)
      }
    }
  }
}
