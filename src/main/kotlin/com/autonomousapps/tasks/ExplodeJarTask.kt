// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.analysis.JarExploder
import com.autonomousapps.internal.analysis.JarExploderConfigurer
import com.autonomousapps.internal.utils.bufferWriteJsonMap
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromNullableJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.intermediates.producer.AndroidLinterDependency
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
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

  /** `kotlin-metadata-jvm`, added to the isolated worker classpath. */
  @get:Classpath
  public abstract val kotlinMetadataClasspath: ConfigurableFileCollection

  /** [`Set<PhysicalArtifact>`][com.autonomousapps.model.internal.PhysicalArtifact]. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  public abstract val physicalArtifacts: RegularFileProperty

  /** [`Set<AndroidLinterDependency>?`][AndroidLinterDependency] */
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val androidLinters: RegularFileProperty

  /** [`Set<ExplodedJar>`][com.autonomousapps.model.internal.intermediates.producer.ExplodedJar]. */
  @get:OutputFile
  public abstract val output: RegularFileProperty

  /** [`Map<Coordinates, Set<BinaryClass>>`][com.autonomousapps.model.internal.intermediates.producer.BinaryClass]. */
  @get:OutputFile
  public abstract val outputBinaryClasses: RegularFileProperty

  @TaskAction public fun action() {
    val explodedJarsOutput = output.getAndDelete()
    val binaryClassesOutput = outputBinaryClasses.getAndDelete()

    val configurer = JarExploderConfigurer(
      temporaryDir = temporaryDir,
      artifacts = physicalArtifacts,
      cache = inMemoryCache.get(),
      explodedJarsOutput = explodedJarsOutput,
      binaryClassesOutput = binaryClassesOutput,
    )

    workerExecutor.classLoaderIsolation {
      // kotlin-metadata-jvm is not on the main plugin classpath (issue 1671); add it for the isolated worker only.
      it.classpath.from(kotlinMetadataClasspath)
    }.submit(ExplodeJarWorkAction::class.java) {
      it.physicalArtifacts.set(configurer.artifactsToAnalyze)
      it.newCacheEntries.set(configurer.newEntriesFile)
      it.androidLinters.set(androidLinters)
    }

    // Block so we can merge the worker's results back into the shared cache.
    workerExecutor.await()
    configurer.finalizeTask()
  }

  public interface ExplodeJarParameters : WorkParameters {
    public val physicalArtifacts: RegularFileProperty

    /** This may be empty. */
    public val androidLinters: RegularFileProperty

    /** [`Map<String, ExplodedJar>`][ExplodedJar] of cache misses computed by this worker, for the task to merge back. */
    public val newCacheEntries: RegularFileProperty
  }

  public abstract class ExplodeJarWorkAction : WorkAction<ExplodeJarParameters> {

    override fun execute() {
      val newCacheEntries = parameters.newCacheEntries.getAndDelete()

      val artifacts = parameters.physicalArtifacts.fromJsonList<PhysicalArtifact>(compressed = true)
      val exploder = JarExploder(
        artifacts = artifacts,
        androidLinters = parameters.androidLinters.fromNullableJsonSet<AndroidLinterDependency>(),
      )

      newCacheEntries.bufferWriteJsonMap(exploder.newEntries, compress = true)
    }
  }
}
