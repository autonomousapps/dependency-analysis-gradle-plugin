package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.AndroidManifestCapability
import com.autonomousapps.model.ProjectVariant
import com.autonomousapps.model.intermediates.AndroidScoreVariant
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class AndroidScoreTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Infers if Android project could instead be a JVM project"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val syntheticProject: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputDirectory
  abstract val dependencies: DirectoryProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      syntheticProject.set(this@AndroidScoreTask.syntheticProject)
      dependencies.set(this@AndroidScoreTask.dependencies)
      output.set(this@AndroidScoreTask.output)
    }
  }

  interface Parameters : WorkParameters {
    val syntheticProject: RegularFileProperty
    val dependencies: DirectoryProperty
    val output: RegularFileProperty
  }

  abstract class Action : WorkAction<Parameters> {

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
      val usesAndroidClasses = project.usedClasses.any { it.startsWith("android.") }
      val importsAndroidClasses = project.imports.any { it.startsWith("android.") }
      val hasAndroidDependencies = androidDependencies.isNotEmpty()

      val score = AndroidScoreVariant(
        variant = project.variant,
        hasAndroidAssets = hasAndroidAssets,
        hasAndroidRes = hasAndroidRes,
        hasBuildConfig = hasBuildConfig,
        usesAndroidClasses = usesAndroidClasses || importsAndroidClasses,
        hasAndroidDependencies = hasAndroidDependencies,
      )

      output.bufferWriteJson(score)
    }
  }
}
