@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.JarExploder
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromNullableJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.intermediates.AndroidLinterDependency
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class ExplodeJarTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Explodes a jar and exposes its capabilities"
  }

  @get:Internal
  abstract val inMemoryCache: Property<InMemoryCache>

  /** Not used by the task action, but necessary for correct input-output tracking, for reasons I do not recall. */
  @get:Classpath
  abstract val compileClasspath: ConfigurableFileCollection

  /** [`Set<PhysicalArtifact>`][com.autonomousapps.model.PhysicalArtifact]. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val physicalArtifacts: RegularFileProperty

  /** [`Set<AndroidLinterDependency>?`][com.autonomousapps.model.intermediates.AndroidLinterDependency] */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val androidLinters: RegularFileProperty

  /** [`Set<ExplodedJar>`][com.autonomousapps.model.intermediates.ExplodedJar]. */
  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(ExplodeJarWorkAction::class.java) {
      inMemoryCache.set(this@ExplodeJarTask.inMemoryCache)
      physicalArtifacts.set(this@ExplodeJarTask.physicalArtifacts)
      androidLinters.set(this@ExplodeJarTask.androidLinters)

      output.set(this@ExplodeJarTask.output)
    }
  }

  interface ExplodeJarParameters : WorkParameters {
    val inMemoryCache: Property<InMemoryCache>
    val physicalArtifacts: RegularFileProperty

    /** This may be empty. */
    val androidLinters: RegularFileProperty

    val output: RegularFileProperty
  }

  abstract class ExplodeJarWorkAction : WorkAction<ExplodeJarParameters> {

    override fun execute() {
      val outputFile = parameters.output.getAndDelete()

      // Actual work
      val explodedJars = JarExploder(
        artifacts = parameters.physicalArtifacts.fromJsonList(),
        androidLinters = parameters.androidLinters.fromNullableJsonSet<AndroidLinterDependency>(),
        inMemoryCache = parameters.inMemoryCache.get()
      ).explodedJars()

      // Write output to disk
      outputFile.bufferWriteJsonSet(explodedJars)
    }
  }
}
