// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.analysis.KotlinMagicFinder
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.internal.InlineMemberCapability
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.TypealiasCapability
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
public abstract class FindKotlinMagicTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Produces a report of dependencies that contribute used inline members"
  }

  @get:Internal
  public abstract val inMemoryCacheProvider: Property<InMemoryCache>

  /** Not used by the task action, but necessary for correct input-output tracking, for reasons I do not recall. */
  @get:Classpath
  public abstract val compileClasspath: ConfigurableFileCollection

  /** `kotlin-metadata-jvm`, added to the isolated worker classpath. */
  @get:Classpath
  public abstract val kotlinMetadataClasspath: ConfigurableFileCollection

  /** [PhysicalArtifact]s used to compile this project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  public abstract val artifacts: RegularFileProperty

  /** Inline members in this project's dependencies. */
  @get:OutputFile
  public abstract val outputInlineMembers: RegularFileProperty

  /** typealiases in this project's dependencies. */
  @get:OutputFile
  public abstract val outputTypealiases: RegularFileProperty

  /**
   * Errors analyzing class files.
   *
   * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1035">Issue 1035</a>
   * @see <a href="https://youtrack.jetbrains.com/issue/KT-60870">KT-60870</a>
   */
  @get:OutputFile
  public abstract val outputErrors: RegularFileProperty

  @TaskAction
  public fun action() {
    // Pass the shared cache content to the work action, which requires serializable data only
    val cache = inMemoryCacheProvider.get()
    val seed = artifacts.fromJsonList<PhysicalArtifact>()
      .mapNotNull { artifact ->
        val key = artifact.cacheKey()
        cache.kotlinCapabilities(key)?.let { key to it }
      }
      .toMap()

    val seedFile = File(temporaryDir, "kotlin-magic-cache-seed.json").apply { bufferWriteJsonMap(seed) }
    val newEntriesFile = File(temporaryDir, "kotlin-magic-cache-new.json")

    workerExecutor.classLoaderIsolation {
      // kotlin-metadata-jvm is not on the main plugin classpath (issue 1671); add it for the isolated worker only.
      it.classpath.from(kotlinMetadataClasspath)
    }.submit(Action::class.java) {
      it.artifacts.set(artifacts)
      it.inlineUsageReport.set(outputInlineMembers)
      it.typealiasReport.set(outputTypealiases)
      it.errorsReport.set(outputErrors)
      it.cacheSeed.set(seedFile)
      it.newCacheEntries.set(newEntriesFile)
    }

    // Block so we can merge the worker's results back into the shared cache.
    workerExecutor.await()
    newEntriesFile.fromJsonMap<String, KotlinCapabilities>().forEach { (key, capabilities) ->
      cache.inlineMembers(key, capabilities)
    }
  }

  public interface Parameters : WorkParameters {
    public val artifacts: RegularFileProperty
    public val inlineUsageReport: RegularFileProperty
    public val typealiasReport: RegularFileProperty
    public val errorsReport: RegularFileProperty

    /** [`Map<String, KotlinCapabilities>`][KotlinCapabilities] of already-cached results, keyed by artifact path. */
    public val cacheSeed: RegularFileProperty

    /** [`Map<String, KotlinCapabilities>`][KotlinCapabilities] of cache misses, for the task to merge back. */
    public val newCacheEntries: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    private val logger = getLogger<FindKotlinMagicTask>()

    override fun execute() {
      val inlineUsageReportFile = parameters.inlineUsageReport.getAndDelete()
      val typealiasReportFile = parameters.typealiasReport.getAndDelete()
      val errorsReport = parameters.errorsReport.getAndDelete()
      val newCacheEntries = parameters.newCacheEntries.getAndDelete()

      val finder = KotlinMagicFinder(
        seedCache = parameters.cacheSeed.fromJsonMap(),
        artifacts = parameters.artifacts.fromJsonList<PhysicalArtifact>(),
        errorsReport = errorsReport,
      )
      val inlineMembers = finder.inlineMembers
      val typealiases = finder.typealiases

      inlineUsageReportFile.bufferWriteJsonSet(inlineMembers)
      typealiasReportFile.bufferWriteJsonSet(typealiases)

      newCacheEntries.bufferWriteJsonMap(finder.newEntries)

      if (finder.didWriteErrors) {
        logger.warn("There were errors during inline member analysis. See ${errorsReport.toPath().toUri()}")
      } else {
        // This file must always exist, even if empty
        errorsReport.writeText("")
      }
    }
  }
}

internal class KotlinCapabilities(
  val inlineMembers: Set<InlineMemberCapability.InlineMember>,
  val typealiases: Set<TypealiasCapability.Typealias>,
) {
  companion object {
    val EMPTY = KotlinCapabilities(emptySet(), emptySet())
  }
}
