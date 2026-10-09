// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.internal.intermediates.producer.NativeLibDependency
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
public abstract class FindNativeLibsTask : DefaultTask() {

  init {
    description = "Produces a report of all dependencies that supply native libs"
  }

  @get:Optional // Only available on Android
  @get:Input
  public abstract val androidJniIds: ListProperty<ComponentArtifactIdentifier>

  @get:Optional // Only available on Android
  @get:Input
  public abstract val androidJniVariants: ListProperty<ResolvedVariantResult>

  @get:Optional // Only available on Android
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val androidJniFiles: ListProperty<File>

  internal fun withAndroidJni(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    androidJniIds.set(Artifact.ids(artifacts))
    androidJniVariants.set(Artifact.variants(artifacts))
    androidJniFiles.set(Artifact.files(artifacts))
  }

  @get:Optional // Only available on JVM
  @get:Input
  public abstract val dylibsIds: ListProperty<ComponentArtifactIdentifier>

  @get:Optional // Only available on JVM
  @get:Input
  public abstract val dylibsVariants: ListProperty<ResolvedVariantResult>

  @get:Optional // Only available on JVM
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val dylibsFiles: ListProperty<File>

  internal fun withDylibs(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    dylibsIds.set(Artifact.ids(artifacts))
    dylibsVariants.set(Artifact.variants(artifacts))
    dylibsFiles.set(Artifact.files(artifacts))
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
    return Artifact.sequenced(androidJniIds, androidJniVariants, androidJniFiles)
      .mapNotNull { artifact ->
        val soFiles = artifact.file.walkBottomUp()
          .filter { it.isFile }
          .map { it.name }
          .toSortedSet()
        try {
          NativeLibDependency.newInstance(
            coordinates = artifact.toCoordinates(),
            fileNames = soFiles,
          )
        } catch (_: GradleException) {
          null
        }
      }
      .toSortedSet()
  }

  private fun findMacNativeDependencies(): Set<NativeLibDependency> {
    return Artifact.sequenced(dylibsIds, dylibsVariants, dylibsFiles)
      .mapNotNull { artifact ->
        val dylibs = artifact.file.walkBottomUp()
          .filter { it.isFile }
          .map { it.name }
          .filter { it.endsWith(".dylib") }
          .toSortedSet()

        if (dylibs.isNotEmpty()) {
          try {
            NativeLibDependency.newInstance(
              coordinates = artifact.toCoordinates(),
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
