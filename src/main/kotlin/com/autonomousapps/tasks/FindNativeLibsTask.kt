// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.ArtifactDetails
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.internal.intermediates.producer.NativeLibDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
public abstract class FindNativeLibsTask : DefaultTask() {

  init {
    description = "Produces a report of all dependencies that supply native libs"
  }

  @get:Optional // Only available on Android
  @get:Nested
  public abstract val androidJniDetails: ListProperty<ArtifactDetails>

  @get:Optional // Only available on Android
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val androidJniFiles: ListProperty<File>

  internal fun withAndroidJni(androidJni: Provider<Set<ResolvedArtifactResult>>) {
    androidJniDetails.set(ArtifactDetails.of(androidJni))
    androidJniFiles.set(ArtifactDetails.files(androidJni))
  }

  @get:Optional // Only available on JVM
  @get:Nested
  public abstract val dylibsDetails: ListProperty<ArtifactDetails>

  @get:Optional // Only available on JVM
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val dylibsFiles: ListProperty<File>

  internal fun withDylibs(dylibs: Provider<Set<ResolvedArtifactResult>>) {
    dylibsDetails.set(ArtifactDetails.of(dylibs))
    dylibsFiles.set(ArtifactDetails.files(dylibs))
  }

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val outputFile = output.getAndDelete()

    val nativeLibDependencies = findAndroidNativeDependencies()
    val macNativeLibs = findMacNativeDependencies()

    val result = nativeLibDependencies + macNativeLibs
    outputFile.bufferWriteJsonSet(result)
  }

  private fun findAndroidNativeDependencies(): Set<NativeLibDependency> {
    return ArtifactDetails.sequenced(androidJniDetails, androidJniFiles)
      .mapNotNull { (details, file) ->
        val soFiles = file.walkBottomUp()
          .filter { it.isFile }
          .map { it.name }
          .toSortedSet()
        try {
          NativeLibDependency.newInstance(
            coordinates = details.toCoordinates(),
            fileNames = soFiles,
          )
        } catch (_: GradleException) {
          null
        }
      }
      .toSortedSet()
  }

  private fun findMacNativeDependencies(): Set<NativeLibDependency> {
    return ArtifactDetails.sequenced(dylibsDetails, dylibsFiles)
      .mapNotNull { (details, file) ->
        val dylibs = file.walkBottomUp()
          .filter { it.isFile }
          .map { it.name }
          .filter { it.endsWith(".dylib") }
          .toSortedSet()

        if (dylibs.isNotEmpty()) {
          try {
            NativeLibDependency.newInstance(
              coordinates = details.toCoordinates(),
              fileNames = dylibs,
            )
          } catch (_: GradleException) {
            null
          }
        } else {
          null
        }
      }
      .toSortedSet()
  }
}
