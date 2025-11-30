// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.model.internal.KtFile
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.intermediates.producer.AndroidLinterDependency
import com.autonomousapps.model.internal.intermediates.ExplodingJar
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.ExplodeJarTask

internal class JarExploder(
  private val artifacts: List<PhysicalArtifact>,
  private val androidLinters: Set<AndroidLinterDependency>,
  private val inMemoryCache: InMemoryCache
) {

  private val logger = getLogger<ExplodeJarTask>()

  fun explodedJars(): Set<ExplodedJar> {
    return artifacts.asSequence()
      .filter {
        // We know how to analyze jars, and directories containing class files
        it.isJar() || it.containsClassFiles()
      }
      .toExplodedJars()
  }

  private fun Sequence<PhysicalArtifact>.toExplodedJars(): Set<ExplodedJar> =
    map { artifact ->
      ExplodedJar(
        artifact = artifact,
        exploding = explode(artifact)
      )
    }.toSortedSet()

  /**
   * Analyzes bytecode in order to extract class names and some basic structural information from
   * the jar ([PhysicalArtifact.file]).
   *
   * With Gradle 8.0+, local java-library project dependencies are provided as a collection of class files rather than
   * jars. It seems that the behavior when requesting the "android-classes" artifact view has changed (previously we'd
   * get jars, but now we get class files).
   */
  private fun explode(artifact: PhysicalArtifact): ExplodingJar {
    val entry = findInCache(artifact)
    if (entry != null) return entry

    return artifact.withContent { content ->

      val ktFiles: Set<KtFile> = content.ktFiles()

      val visitors = content.asSequenceOfClassFiles().map { classFile ->
        ClassNameAndAnnotationsVisitor(logger).apply {
          val reader = ClassReader(classFile.readBytes())
          reader.accept(this, 0)
        }
      }

      val analyzedClasses = visitors.map { it.getAnalyzedClass() }
        .filterNot {
          // Filter out `java` packages, but not `javax`
          it.className.startsWith("java.")
        }
        .toSortedSet()

      ExplodingJar(
        analyzedClasses = analyzedClasses,
        ktFiles = ktFiles,
        androidLintRegistry = findAndroidLinter(artifact)
      ).also { putInCache(artifact, it) }
    }
  }

  private fun findInCache(artifact: PhysicalArtifact): ExplodingJar? {
    return inMemoryCache.explodedJar(artifact.file.absolutePath)
  }

  private fun putInCache(artifact: PhysicalArtifact, explodingJar: ExplodingJar) {
    inMemoryCache.explodedJars(artifact.file.absolutePath, explodingJar)
  }

  private fun findAndroidLinter(physicalArtifact: PhysicalArtifact): String? {
    return androidLinters.find { it.coordinates == physicalArtifact.coordinates }?.lintRegistry
  }
}
