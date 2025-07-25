// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToOrderedSet
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.internal.intermediates.NativeLibDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
public abstract class FindNativeLibsTask : DefaultTask() {

  init {
    description = "Produces a report of all dependencies that supply native libs"
  }

  private lateinit var androidJni: ArtifactCollection

  public fun setAndroidJni(androidJni: ArtifactCollection) {
    this.androidJni = androidJni
  }

  @Optional // Only available on Android
  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  public fun getAndroidJniFiles(): FileCollection? {
    if (!::androidJni.isInitialized) return null
    return androidJni.artifactFiles
  }

  private lateinit var dylibs: ArtifactCollection

  public fun setMacNativeLibs(dylibs: ArtifactCollection) {
    this.dylibs = dylibs
  }

  @Optional // Only available on JVM
  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  public fun getMacNativeLibs(): FileCollection? {
    if (!::dylibs.isInitialized) return null
    return dylibs.artifactFiles
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
    if (!::androidJni.isInitialized) return emptySet()

    return androidJni.mapNotNullToOrderedSet { jniDep ->
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
    if (!::dylibs.isInitialized) return emptySet()

    return dylibs.mapNotNullToOrderedSet { maybeMacArtifact ->
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
