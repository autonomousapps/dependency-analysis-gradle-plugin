// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.JarExploder
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromNullableJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.intermediates.AndroidLinterDependency
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
public abstract class ExplodeJarTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Explodes a jar and exposes its capabilities"
  }

  @get:Internal
  public abstract val inMemoryCache: Property<InMemoryCache>

  /** Not used by the task action, but necessary for correct input-output tracking, for reasons I do not recall. */
  @get:Classpath
  public abstract val compileClasspath: ConfigurableFileCollection

  /** [`Set<PhysicalArtifact>`][com.autonomousapps.model.internal.PhysicalArtifact]. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  public abstract val physicalArtifacts: RegularFileProperty

  /** [`Set<AndroidLinterDependency>?`][com.autonomousapps.model.internal.intermediates.AndroidLinterDependency] */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val androidLinters: RegularFileProperty

  /** [`Set<ExplodedJar>`][com.autonomousapps.model.internal.intermediates.producer.ExplodedJar]. */
  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(ExplodeJarWorkAction::class.java) {
      inMemoryCache.set(this@ExplodeJarTask.inMemoryCache)
      physicalArtifacts.set(this@ExplodeJarTask.physicalArtifacts)
      androidLinters.set(this@ExplodeJarTask.androidLinters)

      output.set(this@ExplodeJarTask.output)
    }
  }

  public interface ExplodeJarParameters : WorkParameters {
    public val inMemoryCache: Property<InMemoryCache>
    public val physicalArtifacts: RegularFileProperty

    /** This may be empty. */
    public val androidLinters: RegularFileProperty

    public val output: RegularFileProperty
  }

  public abstract class ExplodeJarWorkAction : WorkAction<ExplodeJarParameters> {

    override fun execute() {
      val outputFile = parameters.output.getAndDelete()

      val explodedJars = JarExploder(
        artifacts = parameters.physicalArtifacts.fromJsonList(),
        androidLinters = parameters.androidLinters.fromNullableJsonSet<AndroidLinterDependency>(),
        inMemoryCache = parameters.inMemoryCache.get()
      ).explodedJars()

      outputFile.bufferWriteJsonSet(explodedJars, compress = true)
    }
  }
}
