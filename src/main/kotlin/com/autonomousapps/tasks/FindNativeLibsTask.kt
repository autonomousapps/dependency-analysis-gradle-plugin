package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.DependencyConfiguration
import com.autonomousapps.internal.NativeLibDependency
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class FindNativeLibsTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all dependencies that supply native libs"
  }

  private lateinit var artifacts: ArtifactCollection

  fun setArtifacts(artifacts: ArtifactCollection) {
    this.artifacts = artifacts
  }

  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  fun getArtifactFiles(): FileCollection = artifacts.artifactFiles

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val dependencyConfigurations: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val nativeLibs = getArtifactFiles().asFileTree.files.mapToSet { it.name }
    val candidates = dependencyConfigurations.fromJsonSet<DependencyConfiguration>()

    val artifacts = artifacts.mapNotNull {
      try {
        NativeLibDependency(
          componentIdentifier = it.id.componentIdentifier,
          candidates = candidates,
          fileNames = nativeLibs
        )
      } catch (e: GradleException) {
        null
      }
    }

    outputFile.writeText(artifacts.toJson())
  }
}
