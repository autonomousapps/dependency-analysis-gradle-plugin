// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.asSequenceOfClassFiles
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.internal.KtFile
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.PhysicalArtifact.Mode
import com.autonomousapps.model.internal.intermediates.ExplodingJar
import com.autonomousapps.model.internal.intermediates.producer.AndroidLinterDependency
import com.autonomousapps.model.internal.intermediates.producer.ExpensiveJar
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.autonomousapps.tasks.ExplodeJarTask
import java.util.zip.ZipFile

/**
 * Explodes the artifacts it is given, which are only ever the ones [ExplodeJarTask] could not serve from its
 * build-scoped cache. Cache hits never reach this class, and so never cross the worker boundary.
 */
internal class JarExploder(
  private val artifacts: List<PhysicalArtifact>,
  private val androidLinters: Set<AndroidLinterDependency>,
) {

  private val logger = getLogger<ExplodeJarTask>()

  /** [ExpensiveJar]s keyed by artifact path, for [ExplodeJarTask] to merge into its cache. */
  fun expensiveJars(): Map<String, ExpensiveJar> = artifacts.associate { artifact ->
    val explodingJar = explode(artifact, artifact.mode)

    artifact.file.absolutePath to ExpensiveJar(
      coordinates = artifact.coordinates,
      explodedJar = ExplodedJar(
        artifact = artifact,
        exploding = explodingJar,
      ),
      binaryClasses = explodingJar.binaryClasses,
    )
  }

  /**
   * Analyzes bytecode in order to extract class names and some basic structural information from
   * the jar ([PhysicalArtifact.file]).
   *
   * With Gradle 8.0+, local java-library project dependencies are provided as a collection of class files rather than
   * jars. It seems that the behavior when requesting the "android-classes" artifact view has changed (previously we'd
   * get jars, but now we get class files).
   */
  private fun explode(artifact: PhysicalArtifact, mode: Mode): ExplodingJar {
    val ktFiles: Set<KtFile>

    val visitors = when (mode) {
      Mode.ZIP -> {
        ZipFile(artifact.file).use { zip ->
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
        ktFiles = KtFile.fromDirectory(artifact.file)

        artifact.file.asSequenceOfClassFiles()
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

/**
 * Combines the [cacheHits] with the [newEntries] just computed by the worker, into the reports for [artifacts].
 *
 * Runs in the daemon, so a cache hit is reused by reference and is never copied.
 */
internal fun mergeExpensiveJars(
  artifacts: List<PhysicalArtifact>,
  cacheHits: Map<String, ExpensiveJar>,
  newEntries: Map<String, ExpensiveJar>,
): Set<ExpensiveJar> = artifacts.mapToOrderedSet { artifact ->
  val key = artifact.file.absolutePath
  // A cache hit reuses the file-content-derived analysis, but the cached ExpensiveJar also carries the coordinates of
  // whichever artifact first populated this path in the build-scoped cache. Rebind to THIS artifact's identity;
  // otherwise a file shared by two dependencies (e.g. a classifier variant resolved by multiple projects) leaks the
  // other's coordinates and produces wrong advice. Note that Gradle does not provide the classifier in any public API,
  // so `Coordinates` does not (cannot?) model it.
  // tl;dr: two Coordinates, one physical artifact.
  (cacheHits[key] ?: newEntries.getValue(key)).withCoordinates(artifact.coordinates)
}
