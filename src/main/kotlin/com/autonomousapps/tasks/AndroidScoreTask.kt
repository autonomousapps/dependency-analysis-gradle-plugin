// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.AndroidManifestCapability
import com.autonomousapps.model.internal.ProjectVariant
import com.autonomousapps.model.internal.intermediates.AndroidScoreVariant
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
public abstract class AndroidScoreTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    description = "Infers if Android project could instead be a JVM project"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val syntheticProject: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputDirectory
  public abstract val dependencies: DirectoryProperty

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      syntheticProject.set(this@AndroidScoreTask.syntheticProject)
      dependencies.set(this@AndroidScoreTask.dependencies)
      output.set(this@AndroidScoreTask.output)
    }
  }

  public interface Parameters : WorkParameters {
    public val syntheticProject: RegularFileProperty
    public val dependencies: DirectoryProperty
    public val output: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    private val project = parameters.syntheticProject.fromJson<ProjectVariant>()
    private val dependencies = project.dependencies(parameters.dependencies.get())

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val androidDependencies = dependencies.asSequence()
        .filter { it.capabilities.values.find { c -> c is AndroidManifestCapability } != null }
        .map { it.coordinates }
        .toSet()

      val hasAndroidAssets = project.androidAssetsSource.isNotEmpty()
      val hasAndroidRes = project.androidResSource
        // The existence of a manifest is not sufficient to say a project "has android res"
        .filterNot { it.relativePath.endsWith("AndroidManifest.xml") }
        .isNotEmpty()
      val hasBuildConfig = project.codeSource.any { it.relativePath.endsWith("BuildConfig.class") }
      val usesAndroidClasses = project.usedNonAnnotationClasses.any { it.startsWith("android.") }
      val importsAndroidClasses = project.imports.any { it.startsWith("android.") }
      val hasAndroidDependencies = androidDependencies.isNotEmpty()
      val hasBuildTypeSourceSplits = project.codeSource.any { !it.relativePath.startsWith("src/main") }

      val score = AndroidScoreVariant(
        sourceKind = project.sourceKind,
        hasAndroidAssets = hasAndroidAssets,
        hasAndroidRes = hasAndroidRes,
        hasBuildConfig = hasBuildConfig,
        usesAndroidClasses = usesAndroidClasses || importsAndroidClasses,
        hasAndroidDependencies = hasAndroidDependencies,
        hasBuildTypeSourceSplits = hasBuildTypeSourceSplits
      )

      output.bufferWriteJson(score)
    }
  }
}
