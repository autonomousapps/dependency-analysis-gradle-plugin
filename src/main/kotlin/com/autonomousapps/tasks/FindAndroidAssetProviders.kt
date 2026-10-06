// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.ArtifactDetails
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.internal.intermediates.producer.AndroidAssetDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedArtifactResult
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

  @get:Nested
  public abstract val assetDetails: ListProperty<ArtifactDetails>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val assetFiles: ListProperty<File>

  internal fun withAssets(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    assetDetails.set(ArtifactDetails.of(artifacts))
    assetFiles.set(ArtifactDetails.files(artifacts))
  }

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val outputFile = output.getAndDelete()

    val assetProviders: Set<AndroidAssetDependency> = ArtifactDetails.sequenced(assetDetails, assetFiles)
      // Sometimes the file doesn't exist. Is this a bug? A feature? Who knows?
      // We only want non-empty directories.
      .filter { (_, file) -> file.exists() }
      .filter { (_, file) -> file.isDirectory }
      .filter { (_, file) -> file.listFiles()!!.isNotEmpty() }
      .mapNotNull { (detail, dir) ->
        try {
          val assets = dir.listFiles()!!.map {
            it.toRelativeString(dir)
          }
          AndroidAssetDependency.newInstance(
            coordinates = detail.toCoordinates(),
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
