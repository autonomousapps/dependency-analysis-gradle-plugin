// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.ArtifactDetails
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToOrderedSet
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
    androidJniDetails.set(androidJni.map {
      it.map { artifact -> ArtifactDetails(artifact.id, artifact.variant) }
    })
    androidJniFiles.set(androidJni.map {
      it.map { artifact -> artifact.file }
    })
  }

  @get:Optional // Only available on JVM
  @get:Nested
  public abstract val dylibsDetails: ListProperty<ArtifactDetails>

  @get:Optional // Only available on JVM
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val dylibsFiles: ListProperty<File>

  internal fun withDylibs(dylibs: Provider<Set<ResolvedArtifactResult>>) {
    dylibsDetails.set(dylibs.map {
      it.map { artifact -> ArtifactDetails(artifact.id, artifact.variant) }
    })
    dylibsFiles.set(dylibs.map {
      it.map { artifact -> artifact.file }
    })
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
    if (!androidJniDetails.isPresent) return emptySet()

    val details = androidJniDetails.get()
    val files = androidJniFiles.get()
    require(details.size == files.size) {
      "Expected 'details.size == files.size'. Got details.size=${details.size}, files.size=${files.size}"
    }

    return details.zip(files).mapNotNullToOrderedSet { (details, file) ->
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
  }

  private fun findMacNativeDependencies(): Set<NativeLibDependency> {
    if (!dylibsDetails.isPresent) return emptySet()

    val details = dylibsDetails.get()
    val files = dylibsFiles.get()
    require(details.size == files.size) {
      "Expected 'details.size == files.size'. Got details.size=${details.size}, files.size=${files.size}"
    }

    return details.zip(files).mapNotNullToOrderedSet { (details, file) ->
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
  }
}
