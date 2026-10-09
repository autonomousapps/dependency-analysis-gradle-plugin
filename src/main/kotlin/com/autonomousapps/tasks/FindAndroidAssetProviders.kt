// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.internal.intermediates.producer.AndroidAssetDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
public abstract class FindAndroidAssetProviders : DefaultTask() {

  init {
    description = "Produces a report of dependencies that supply Android assets"
  }

  @get:Input
  public abstract val assetIds: ListProperty<ComponentArtifactIdentifier>

  @get:Input
  public abstract val assetVariants: ListProperty<ResolvedVariantResult>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val assetFiles: ListProperty<File>

  internal fun withAssets(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    assetIds.set(Artifact.ids(artifacts))
    assetVariants.set(Artifact.variants(artifacts))
    assetFiles.set(Artifact.files(artifacts))
  }

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val outputFile = output.getAndDelete()

    val assetProviders: Set<AndroidAssetDependency> = Artifact.sequenced(assetIds, assetVariants, assetFiles)
      // Sometimes the file doesn't exist. Is this a bug? A feature? Who knows?
      // We only want non-empty directories.
      .filter { it.file.exists() }
      .filter { it.file.isDirectory }
      .filter { it.file.listFiles()!!.isNotEmpty() }
      .mapNotNull { artifact ->
        try {
          val dir = artifact.file
          val assets = dir.listFiles()!!.map { f ->
            f.toRelativeString(dir)
          }
          AndroidAssetDependency.newInstance(
            coordinates = artifact.toCoordinates(),
            assets = assets
          )
        } catch (_: GradleException) {
          null
        }
      }
      .toSortedSet()

    outputFile.bufferWriteJsonSet(assetProviders)
  }
}
