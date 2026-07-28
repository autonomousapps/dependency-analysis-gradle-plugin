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

  /**
   * [ExplodedJar]s computed during this run (cache misses), keyed by artifact coordinates, to merge back into the
   * cache.
   */
  val newEntries: MutableMap<String, ExpensiveJar> = LinkedHashMap()

  private val expensiveJars = artifacts.asSequence()
    .filter {
      // We know how to analyze jars, and directories containing class files
      it.isJar() || it.containsClassFiles()
    }
    .toExpensiveJars()

  fun binaryClasses(): Map<Coordinates, Set<BinaryClass>> {
    return expensiveJars.associate { it.coordinates to it.binaryClasses }.toSortedMap().efficient()
  }

  fun explodedJars(): Set<ExplodedJar> {
    return expensiveJars.mapToOrderedSet { it.explodedJar }
  }

  private fun Sequence<PhysicalArtifact>.toExpensiveJars(): Set<ExpensiveJar> =
    map { artifact ->
      val key = artifact.coordinates.toString()
      // A cache hit reuses the file-content-derived analysis, but the cached ExplodedJar also carries the coordinates
      // of whichever artifact first populated this path in the build-scoped cache. Rebind to THIS artifact's identity;
      // otherwise a file shared by two dependencies (e.g. a classifier/capability variant resolved by multiple
      // projects) leaks the other's coordinates and produces wrong advice.
      seedCache[key]?.copy(coordinates = artifact.coordinates) ?: run {
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
