// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.analysis.KotlinMagicFinder
import com.autonomousapps.internal.analysis.partitionBy
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.internal.InlineMemberCapability
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.TypealiasCapability
import com.autonomousapps.model.internal.intermediates.producer.InlineMemberDependency
import com.autonomousapps.model.internal.intermediates.producer.TypealiasDependency
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

  // TODO(tsr): should I follow the same JarExploderConfigurer pattern I implemented for ExplodeJarTask? Not sure how
  //  much I like that.
  @TaskAction
  public fun action() {
    val inlineMembersOutput = outputInlineMembers.getAndDelete()
    val typeAliasesOutput = outputTypealiases.getAndDelete()

    // Pass the shared cache content to the work action, which requires serializable data only
    val cache = inMemoryCacheProvider.get()

    val artifacts = artifacts.fromJsonList<PhysicalArtifact>().filter(PhysicalArtifact::isValidArtifact)
    // We only need to pass in the misses for analysis. This avoids the redundant de/serialization round-trip for the
    // hits. Get the full set of artifacts, then partition into hits & misses. Finally, write misses (things to be
    // analyzed) into work action input file.
    val (hits, misses) = artifacts.partitionBy(
      { it.cacheKey() },
      { k -> cache.kotlinCapabilities(k) },
    )

    // Work action output (new entries to be merged with old).
    val newEntriesFile = File(temporaryDir, "kotlin-magic-cache-new.json")
    // PhysicalArtifacts that haven't yet been analyzed (can't be found in the cache).
    val artifactsToAnalyze = temporaryDir.resolve("artifacts-misses.json.gz")
      .apply { bufferWriteJsonList(misses, compress = true) }

    workerExecutor.classLoaderIsolation {
      // kotlin-metadata-jvm is not on the main plugin classpath (issue 1671); add it for the isolated worker only.
      it.classpath.from(kotlinMetadataClasspath)
    }.submit(Action::class.java) {
      it.physicalArtifacts.set(artifactsToAnalyze)
      it.errorsReport.set(outputErrors)
      it.newCacheEntries.set(newEntriesFile)
    }

    // Block so we can merge the worker's results back into the shared cache.
    workerExecutor.await()

    val newEntries = newEntriesFile.fromJsonMap<String, KotlinCapabilities>()
    newEntries.forEach { (key, capabilities) -> cache.inlineMembers(key, capabilities) }

    // merge new with old for writing out as task outputs
    val inlineMembers = sortedSetOf<InlineMemberDependency>()
    val typealiases = sortedSetOf<TypealiasDependency>()
    artifacts.forEach { a ->
      val key = a.cacheKey()
      val capabilities = (hits[key] ?: newEntries[key]) ?: error("Missing value for '$key'.")
      if (capabilities.inlineMembers.isNotEmpty()) {
        inlineMembers += InlineMemberDependency.newInstance(a.coordinates, capabilities.inlineMembers)
      }
      if (capabilities.typealiases.isNotEmpty()) {
        typealiases += TypealiasDependency.newInstance(a.coordinates, capabilities.typealiases)
      }
    }

    // Finally, write output
    inlineMembersOutput.bufferWriteJsonSet(inlineMembers)
    typeAliasesOutput.bufferWriteJsonSet(typealiases)
  }

  public interface Parameters : WorkParameters {
    public val physicalArtifacts: RegularFileProperty
    public val errorsReport: RegularFileProperty

    /** [`Map<String, KotlinCapabilities>`][KotlinCapabilities] of cache misses, for the task to merge back. */
    public val newCacheEntries: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    private val logger = getLogger<FindKotlinMagicTask>()

    override fun execute() {
      val newCacheEntries = parameters.newCacheEntries.getAndDelete()
      val errorsReport = parameters.errorsReport.getAndDelete()

      val finder = KotlinMagicFinder(
        artifacts = parameters.physicalArtifacts.fromJsonList<PhysicalArtifact>(compressed = true),
        errorsReport = errorsReport,
      )

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
