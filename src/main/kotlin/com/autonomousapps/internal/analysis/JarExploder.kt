// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.ClassNameAndAnnotationsVisitor
import com.autonomousapps.internal.ClassNames
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.asSequenceOfClassFiles
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.model.internal.KtFile
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.PhysicalArtifact.Mode
import com.autonomousapps.model.internal.intermediates.ExplodingJar
import com.autonomousapps.model.internal.intermediates.producer.AndroidLinterDependency
import com.autonomousapps.model.internal.intermediates.producer.ExpensiveJar
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.autonomousapps.tasks.ExplodeJarTask
import java.util.zip.ZipFile

internal class JarExploder(
  artifacts: List<PhysicalArtifact>,
  private val androidLinters: Set<AndroidLinterDependency>,
) {

  private val logger = getLogger<ExplodeJarTask>()

  private val _newEntries = linkedMapOf<String, ExpensiveJar>()

  /** [ExplodedJar]s computed during this run (cache misses), keyed by artifact path, to merge back into the cache. */
  val newEntries: Map<String, ExpensiveJar> get() = _newEntries

  init {
    artifacts.asSequence().cacheExpensiveJars()
  }

  private fun Sequence<PhysicalArtifact>.cacheExpensiveJars() {
    forEach { artifact ->
      val key = artifact.cacheKey()

      val explodingJar = if (artifact.isJar()) {
        explode(artifact, Mode.ZIP)
      } else {
        explode(artifact, Mode.CLASSES)
      }

      val explodedJar = ExplodedJar(
        artifact = artifact,
        exploding = explodingJar
      )

      val expensiveJar = ExpensiveJar(
        coordinates = artifact.coordinates,
        explodedJar = explodedJar,
        binaryClasses = explodingJar.binaryClasses,
      )

      // The point of this class
      _newEntries[key] = expensiveJar
    }
  }

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
