// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.identifiers
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToOrderedSet
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.internal.intermediates.producer.NativeLibDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
public abstract class FindNativeLibsTask : DefaultTask() {

  init {
    description = "Produces a report of all dependencies that supply native libs"
  }

  // A ResolvedArtifactResult cannot be a task input
  @get:Internal
  public abstract val androidJni: SetProperty<ResolvedArtifactResult>

  @Optional // Only available on Android
  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  public fun getAndroidJniFiles(): Provider<List<File>>? {
    if (!androidJni.isPresent) return null
    return androidJni.map { artifactResults -> artifactResults.map { it.file } }
  }

  @Input
  public fun getAndroidJniIdentifiers(): Provider<List<String>> = androidJni.identifiers()

  // A ResolvedArtifactResult cannot be a task input
  @get:Internal
  public abstract val dylibs: SetProperty<ResolvedArtifactResult>

  @Optional // Only available on JVM
  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  public fun getMacNativeLibs(): Provider<List<File>>? {
    if (!dylibs.isPresent) return null
    return dylibs.map { artifactResults -> artifactResults.map { it.file } }
  }

  @Input
  public fun getMacNativeLibIdentifiers(): Provider<List<String>> = dylibs.identifiers()

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
    if (!androidJni.isPresent) return emptySet()

    return androidJni.get().mapNotNullToOrderedSet { jniDep ->
      val soFiles = jniDep.file.walkBottomUp()
        .filter { it.isFile }
        .map { it.name }
        .toSortedSet()
      try {
        NativeLibDependency.newInstance(
          coordinates = jniDep.toCoordinates(),
          fileNames = soFiles,
        )
      } catch (_: GradleException) {
        null
      }
    }
  }

  private fun findMacNativeDependencies(): Set<NativeLibDependency> {
    if (!dylibs.isPresent) return emptySet()

    return dylibs.get().mapNotNullToOrderedSet { maybeMacArtifact ->
      val dylibs = maybeMacArtifact.file.walkBottomUp()
        .filter { it.isFile }
        .map { it.name }
        .filter { it.endsWith(".dylib") }
        .toSortedSet()

      if (dylibs.isNotEmpty()) {
        try {
          NativeLibDependency.newInstance(
            coordinates = maybeMacArtifact.toCoordinates(),
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
