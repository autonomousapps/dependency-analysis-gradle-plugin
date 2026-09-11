// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.intermediates.producer.BinaryClass
import com.autonomousapps.model.internal.intermediates.producer.BinaryClasses
import com.autonomousapps.model.internal.intermediates.producer.ExpensiveJar
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.file.RegularFileProperty
import java.io.File

/**
 * Helper class that encapsulates the logic that wires the [ExplodeJarTask][com.autonomousapps.tasks.ExplodeJarTask]
 * work action. We need to communicate with work action via files (nothing by reference, sadly). Inputs/outputs must be
 * serializable.
 */
internal class JarExploderConfigurer(
  temporaryDir: File,
  private val artifacts: List<PhysicalArtifact>,
  private val cache: ExpensiveJarCache,
  private val explodedJarsOutput: File,
  private val binaryClassesOutput: File,
) {

  constructor(
    temporaryDir: File,
    artifacts: RegularFileProperty,
    cache: InMemoryCache,
    explodedJarsOutput: File,
    binaryClassesOutput: File,
  ) : this(
    temporaryDir = temporaryDir,
    artifacts = artifacts.fromJsonList<PhysicalArtifact>().filter(PhysicalArtifact::isValidArtifact),
    cache = DefaultExpensiveJarCache(cache),
    explodedJarsOutput = explodedJarsOutput,
    binaryClassesOutput = binaryClassesOutput,
  )

  interface ExpensiveJarCache {
    fun expensiveJar(name: String): ExpensiveJar?
    fun expensiveJars(name: String, expensiveJar: ExpensiveJar)
  }

  private class DefaultExpensiveJarCache(private val delegate: InMemoryCache) : ExpensiveJarCache {
    override fun expensiveJar(name: String): ExpensiveJar? = delegate.expensiveJar(name)
    override fun expensiveJars(name: String, expensiveJar: ExpensiveJar) {
      delegate.expensiveJars(name, expensiveJar)
    }
  }

  private val hits: Map<String, ExpensiveJar>
  private val misses: List<PhysicalArtifact>

  init {
    // We only need to pass in the misses for analysis. This avoids the redundant de/serialization round-trip for the
    // hits. Get the full set of artifacts, then partition into hits & misses. Finally, write misses (things to be
    // analyzed) into work action input file.
    val (hits, misses) = artifacts.partitionBy(
      { it.cacheKey() },
      { k -> cache.expensiveJar(k) },
    )
    this.hits = hits
    this.misses = misses
  }

  /** Work action output (new entries to be merged with old). */
  val newEntriesFile = temporaryDir.resolve("exploded-jars-cache-new.json.gz")

  /** [PhysicalArtifact]s that haven't yet been analyzed (can't be found in the cache). */

  val artifactsToAnalyze = temporaryDir.resolve("artifacts-misses.json.gz")
    .apply { bufferWriteJsonList(misses, compress = true) }

  /** Call this when the task's work action is complete. */
  fun finalizeTask() {
    val newEntries = newEntriesFile.fromJsonMap<String, ExpensiveJar>(compressed = true)
    newEntries.forEach { (key, expensiveJar) -> cache.expensiveJars(key, expensiveJar) }

    // merge new with old for writing out as task outputs
    val expensiveJars = artifacts.mapToOrderedSet { a ->
      val key = a.cacheKey()
      val expensiveJar = (hits[key] ?: newEntries[key]) ?: error("Missing value for '$key'.")
      expensiveJar.withCoordinates(a.coordinates)
    }
    val explodedJars = expensiveJars.mapToOrderedSet { it.explodedJar }
    val binaryClasses = BinaryClasses.of(expensiveJars.asBinaryClasses())

    // Finally, write output
    explodedJarsOutput.bufferWriteJsonSet(explodedJars, compress = true)
    binaryClassesOutput.bufferWriteJsonSet(binaryClasses, compress = true)
  }

  private fun Set<ExpensiveJar>.asBinaryClasses(): Map<Coordinates, Set<BinaryClass>> {
    val map = sortedMapOf<Coordinates, MutableSet<BinaryClass>>()

    // Account for the fact that multiple artifacts can currently have the same Coordinates. This happens when a
    // dependency has multiple artifacts, including some with classifiers. For example, `org.threeten:threetenbp:1.6.0`
    // has a standard jar, and a jar with a `-no-tzdb` classifier. This functions merges both jars into a single set of
    // BinaryClasses.
    // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1814
    forEach { jar ->
      map.merge(jar.coordinates, jar.binaryClasses.toMutableSet()) { acc, inc ->
        acc.apply { addAll(inc) }
      }
    }

    return map
  }
}
