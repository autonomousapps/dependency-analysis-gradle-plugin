package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.intermediates.AndroidAssetDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

abstract class FindAndroidAssetProviders : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of dependencies that supply Android assets"
  }

  private lateinit var assetDirs: ArtifactCollection

  fun setAssets(assets: ArtifactCollection) {
    this.assetDirs = assets
  }

  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  fun getAssetArtifactFiles(): FileCollection = assetDirs.artifactFiles

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val assetProviders: Set<AndroidAssetDependency> = assetDirs.asSequence()
      // Sometimes the file doesn't exist. Is this a bug? A feature? Who knows?
      // We only want non-empty directories.
      .filter { it.file.exists() }
      .filter { it.file.isDirectory }
      .filter { it.file.listFiles()!!.isNotEmpty() }
      .mapNotNull { artifact ->
        try {
          val dir = artifact.file
          val assets = dir.listFiles()!!.map {
            it.toRelativeString(dir)
          }
          AndroidAssetDependency(
            coordinates = artifact.toCoordinates(),
            assets = assets
          )
        } catch (e: GradleException) {
          null
        }
      }
      .toSortedSet()

    outputFile.bufferWriteJsonSet(assetProviders)
  }
}
