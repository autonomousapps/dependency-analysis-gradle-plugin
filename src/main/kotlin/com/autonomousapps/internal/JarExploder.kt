// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.asSequenceOfClassFiles
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.KtFile
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.PhysicalArtifact.Mode
import com.autonomousapps.model.internal.intermediates.ExplodingJar
import com.autonomousapps.model.internal.intermediates.producer.AndroidLinterDependency
import com.autonomousapps.model.internal.intermediates.producer.BinaryClass
import com.autonomousapps.model.internal.intermediates.producer.ExpensiveJar
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.autonomousapps.tasks.ExplodeJarTask
import java.util.zip.ZipFile

internal class JarExploder(
  artifacts: List<PhysicalArtifact>,
  private val androidLinters: Set<AndroidLinterDependency>,
  private val seedCache: Map<String, ExpensiveJar>,
) {

  private val logger = getLogger<ExplodeJarTask>()

  /** [ExplodedJar]s computed during this run (cache misses), keyed by artifact path, to merge back into the cache. */
  val newEntries: MutableMap<String, ExpensiveJar> = LinkedHashMap()

  private val expensiveJars = artifacts.asSequence()
    .filter {
      // We know how to analyze jars, and directories containing class files
      it.isJar() || it.containsClassFiles()
    }
    .toExpensiveJars()

  fun binaryClasses(): Map<Coordinates, Set<BinaryClass>> {
    val map = sortedMapOf<Coordinates, MutableSet<BinaryClass>>()

    // Account for the fact that multiple artifacts can currently have the same Coordinates. This happens when a
    // dependency has multiple artifacts, including some with classifiers. For example, `org.threeten:threetenbp:1.6.0`
    // has a standard jar, and a jar with a `-no-tzdb` classifier. This functions merges both jars into a single set of
    // `BinaryClass`es.
    // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1814
    expensiveJars.forEach { jar ->
      map.merge(jar.coordinates, jar.binaryClasses.toMutableSet()) { acc, inc ->
        acc.apply { addAll(inc) }
      }
    }

    return map.efficient()
  }

  fun explodedJars(): Set<ExplodedJar> {
    return expensiveJars.mapToOrderedSet { it.explodedJar }
  }

  private fun Sequence<PhysicalArtifact>.toExpensiveJars(): Set<ExpensiveJar> =
    map { artifact ->
      val key = artifact.cacheKey()
      // A cache hit reuses the file-content-derived analysis, but the cached ExpensiveJar also carries the coordinates
      // of whichever artifact first populated this path in the build-scoped cache. Rebind to THIS artifact's identity;
      // otherwise a file shared by two dependencies (e.g. a classifier variant resolved by multiple projects) leaks the
      // other's coordinates and produces wrong advice. Note that Gradle does not provide the classifier in any public
      // API, so `Coordinates` does not (cannot?) model it.
      // tl;dr: two Coordinates, one physical artifact.
      val cached = seedCache[key]
      if (cached != null) {
        cached.withCoordinates(artifact.coordinates)
      } else {
        val explodingJar = if (artifact.isJar()) {
          explode(artifact, Mode.ZIP)
        } else {
          explode(artifact, Mode.CLASSES)
        }

        val explodedJar = ExplodedJar(
          artifact = artifact,
          exploding = explodingJar
        )

        ExpensiveJar(
          coordinates = artifact.coordinates,
          explodedJar = explodedJar,
          binaryClasses = explodingJar.binaryClasses,
        ).also { newEntries[key] = it }
      }
    }.toSortedSet()

  /**
   * Analyzes bytecode in order to extract class names and some basic structural information from the jar or
   * directory(ies) of class files.
   *
   * @see [PhysicalArtifact.files]
   */
  private fun explode(artifact: PhysicalArtifact, mode: Mode): ExplodingJar {
    val ktFiles: Set<KtFile>

    val visitors = when (mode) {
      Mode.ZIP -> {
        ZipFile(artifact.jarFile()).use { zip ->
          ktFiles = KtFile.fromZip(zip)

          zip.asSequenceOfClassFiles()
            .map { classEntry ->
              ClassNameAndAnnotationsVisitor(logger).apply {
                val reader = zip.getInputStream(classEntry).use { ClassReader(it.readBytes()) }
                reader.accept(this, 0)
              }
            }.toList()
        }
      }

      Mode.CLASSES -> {
        ktFiles = KtFile.fromDirectories(artifact.files)

        artifact.classFiles()
          .map { classFile ->
            ClassNameAndAnnotationsVisitor(logger).apply {
              val reader = classFile.inputStream().use { ClassReader(it.readBytes()) }
              reader.accept(this, 0)
            }
          }.toList()
      }
    }

    val analyzedClasses = visitors.map { it.getAnalyzedClass() }
      .filterNot { ClassNames.isCoreJava(it.className) }
      .toSet()

    return ExplodingJar(
      analyzedClasses = analyzedClasses,
      ktFiles = ktFiles,
      androidLintRegistry = findAndroidLinter(artifact)
    )
  }

  private fun findAndroidLinter(physicalArtifact: PhysicalArtifact): String? {
    return androidLinters.find { it.coordinates == physicalArtifact.coordinates }?.lintRegistry
  }
}
