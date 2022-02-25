package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.intermediates.NativeLibDependency
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

  private lateinit var androidJni: ArtifactCollection

  fun setAndroidJni(androidJni: ArtifactCollection) {
    this.androidJni = androidJni
  }

  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  fun getAndroidJniFiles(): FileCollection = androidJni.artifactFiles

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val nativeLibs = getAndroidJniFiles().asFileTree.files.mapToSet { it.name }

    val artifacts = androidJni.mapNotNullToOrderedSet {
      try {
        NativeLibDependency(
          coordinates = it.toCoordinates(),
          fileNames = nativeLibs
        )
      } catch (e: GradleException) {
        null
      }
    }

    outputFile.writeText(artifacts.toJson())
  }
}
