// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.JarExploder
import com.autonomousapps.internal.mergeExpensiveJars
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.bufferWriteJsonList
import com.autonomousapps.internal.utils.bufferWriteJsonMap
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromJsonMap
import com.autonomousapps.internal.utils.fromNullableJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.partitionByCacheHit
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.intermediates.producer.AndroidLinterDependency
import com.autonomousapps.model.internal.intermediates.producer.BinaryClasses
import com.autonomousapps.model.internal.intermediates.producer.ExpensiveJar
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

  /** [`Map<Coordinates, Set<BinaryClass>>`][com.autonomousapps.model.internal.intermediates.producer.BinaryClass]. */
  @get:OutputFile
  public abstract val outputBinaryClasses: RegularFileProperty

  @TaskAction public fun action() {
    val cache = inMemoryCache.get()
    val artifacts = physicalArtifacts.fromJsonList<PhysicalArtifact>()
      // We know how to analyze jars, and directories containing class files.
      .filter { it.isJar() || it.containsClassFiles() }

    // The worker runs in an isolated classloader and so cannot share objects with this JVM; anything we hand it must be
    // serialized out and deserialized back into a full copy. Send it only the artifacts we can't already answer for.
    // Cache hits are held here, by reference, and never copied. (Seeding the worker with them instead cost a per-task
    // deep copy of the whole compile classpath: tens of MB of JSON, serialized and parsed again for every task.)
    val (cacheHits, misses) = artifacts.partitionByCacheHit(
      key = { it.file.absolutePath },
      hit = { cache.expensiveJar(it) },
    )

    val missesFile = File(temporaryDir, "exploded-jars-misses.json").apply { bufferWriteJsonList(misses) }
    val newEntriesFile = File(temporaryDir, "exploded-jars-cache-new.json")

    workerExecutor.classLoaderIsolation {
      // kotlin-metadata-jvm is not on the main plugin classpath (issue 1671); add it for the isolated worker only.
      it.classpath.from(kotlinMetadataClasspath)
    }.submit(ExplodeJarWorkAction::class.java) {
      it.physicalArtifacts.set(missesFile)
      it.androidLinters.set(androidLinters)
      it.newCacheEntries.set(newEntriesFile)
    }

    // Block so we can merge the worker's results back into the shared cache and write the reports.
    workerExecutor.await()

    val newEntries = newEntriesFile.fromJsonMap<String, ExpensiveJar>(compressed = true)
    newEntries.forEach { (key, expensiveJar) -> cache.expensiveJars(key, expensiveJar) }

    val expensiveJars = mergeExpensiveJars(artifacts, cacheHits, newEntries)
    val binaryClasses = BinaryClasses.of(
      expensiveJars.associate { it.coordinates to it.binaryClasses }.toSortedMap().efficient()
    )

    output.getAndDelete().bufferWriteJsonSet(expensiveJars.mapToOrderedSet { it.explodedJar }, compress = true)
    outputBinaryClasses.getAndDelete().bufferWriteJson(binaryClasses, compress = true)
  }

  public interface ExplodeJarParameters : WorkParameters {
    /**
     * [`List<PhysicalArtifact>`][PhysicalArtifact] to explode: the subset of the task's artifacts that missed the
     * build-scoped cache.
     */
    public val physicalArtifacts: RegularFileProperty

    /** This may be empty. */
    public val androidLinters: RegularFileProperty

    /** [`Map<String, ExpensiveJar>`][ExpensiveJar] computed by this worker, for the task to merge back. */
    public val newCacheEntries: RegularFileProperty
  }

  public abstract class ExplodeJarWorkAction : WorkAction<ExplodeJarParameters> {

    override fun execute() {
      val newCacheEntries = parameters.newCacheEntries.getAndDelete()

      val expensiveJars = JarExploder(
        artifacts = parameters.physicalArtifacts.fromJsonList(),
        androidLinters = parameters.androidLinters.fromNullableJsonSet<AndroidLinterDependency>(),
      ).expensiveJars()

      newCacheEntries.bufferWriteJsonMap(expensiveJars, compress = true)
    }
  }
}
