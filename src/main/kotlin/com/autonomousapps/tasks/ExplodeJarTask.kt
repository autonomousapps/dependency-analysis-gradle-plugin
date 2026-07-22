// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.JarExploder
import com.autonomousapps.internal.utils.bufferWriteJsonMap
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromJsonMap
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
import java.io.File
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

  @TaskAction public fun action() {
    // Pass the shared cache content to the work action, which requires serializable data only
    val cache = inMemoryCache.get()
    val seed = physicalArtifacts.fromJsonList<PhysicalArtifact>()
      .mapNotNull { artifact ->
        val key = artifact.file.absolutePath
        cache.explodedJar(key)?.let { key to it }
      }
      .toMap()

    val seedFile = File(temporaryDir, "exploded-jars-cache-seed.json").apply { bufferWriteJsonMap(seed) }
    val newEntriesFile = File(temporaryDir, "exploded-jars-cache-new.json")

    workerExecutor.classLoaderIsolation {
      // kotlin-metadata-jvm is not on the main plugin classpath (issue 1671); add it for the isolated worker only.
      it.classpath.from(kotlinMetadataClasspath)
    }.submit(ExplodeJarWorkAction::class.java) {
      it.physicalArtifacts.set(physicalArtifacts)
      it.androidLinters.set(androidLinters)
      it.output.set(output)
      it.cacheSeed.set(seedFile)
      it.newCacheEntries.set(newEntriesFile)
    }

    // Block so we can merge the worker's results back into the shared cache.
    workerExecutor.await()
    newEntriesFile.fromJsonMap<String, ExplodedJar>().forEach { (key, explodedJar) ->
      cache.explodedJars(key, explodedJar)
    }
  }

  public interface ExplodeJarParameters : WorkParameters {
    public val physicalArtifacts: RegularFileProperty

    /** This may be empty. */
    public val androidLinters: RegularFileProperty

    public val output: RegularFileProperty

    /** [`Map<String, ExplodedJar>`][ExplodedJar] of already-cached results, keyed by artifact path. */
    public val cacheSeed: RegularFileProperty

    /** [`Map<String, ExplodedJar>`][ExplodedJar] of cache misses computed by this worker, for the task to merge back. */
    public val newCacheEntries: RegularFileProperty
  }

  public abstract class ExplodeJarWorkAction : WorkAction<ExplodeJarParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()
      val newCacheEntries = parameters.newCacheEntries.getAndDelete()

      val exploder = JarExploder(
        artifacts = parameters.physicalArtifacts.fromJsonList(),
        androidLinters = parameters.androidLinters.fromNullableJsonSet<AndroidLinterDependency>(),
        seedCache = parameters.cacheSeed.fromJsonMap(),
      )
      val explodedJars = exploder.explodedJars()

      output.bufferWriteJsonSet(explodedJars, compress = true)
      newCacheEntries.bufferWriteJsonMap(exploder.newEntries)
    }
  }
}
