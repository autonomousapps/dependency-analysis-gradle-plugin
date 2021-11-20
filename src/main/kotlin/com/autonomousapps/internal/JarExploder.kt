package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.asClassFiles
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.KtFile
import com.autonomousapps.model.intermediates.AndroidLinterDependency
import com.autonomousapps.model.intermediates.ExplodedJar
import com.autonomousapps.model.intermediates.ExplodingJar
import com.autonomousapps.model.PhysicalArtifact
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.ExplodeJarTask
import java.util.zip.ZipFile

/**
 * Used by [com.autonomousapps.tasks.AnalyzeJarTask].
 */
internal class JarExploder(
  private val artifacts: List<PhysicalArtifact>,
  private val androidLinters: Set<AndroidLinterDependency>,
  private val inMemoryCache: InMemoryCache
) {

  private val logger = getLogger<ExplodeJarTask>()

  fun explodedJars(): Set<ExplodedJar> {
    return artifacts
      .filter { it.file.name.endsWith(".jar") }
      .toExplodedJars()
  }

  private fun Iterable<PhysicalArtifact>.toExplodedJars(): Set<ExplodedJar> =
    mapToOrderedSet { artifact ->
      val explodedJar = explodeJar(artifact)
      ExplodedJar(
        artifact = artifact,
        exploding = explodedJar
      )
    }

  /**
   * Analyzes bytecode in order to extract class names and some basic structural information from
   * the jar ([PhysicalArtifact.file]).
   */
  private fun explodeJar(artifact: PhysicalArtifact): ExplodingJar {
    val zip = ZipFile(artifact.file)

    val alreadyExplodingJar: ExplodingJar? = inMemoryCache.explodedJar(artifact.coordinates.toString())
    if (alreadyExplodingJar != null) {
      return alreadyExplodingJar
    }

    inMemoryCache.updateJars(zip.name)

    val ktFiles = KtFile.fromZip(zip).toSet()
    val analyzedClasses = zip.asClassFiles()
      .map { classEntry ->
        ClassNameAndAnnotationsVisitor(logger).apply {
          val reader = zip.getInputStream(classEntry).use { ClassReader(it.readBytes()) }
          reader.accept(this, 0)
        }
      }
      .map { it.getAnalyzedClass() }
      .filterNot {
        // Filter out `java` packages, but not `javax`
        it.className.startsWith("java/")
      }
      .mapToOrderedSet {
        // TODO also replace "$"?
        it.copy(className = it.className.replace("/", "."))
      }
      .onEach { inMemoryCache.updateClasses(it.className) }

    return ExplodingJar(
      analyzedClasses = analyzedClasses,
      ktFiles = ktFiles,
      androidLintRegistry = findAndroidLinter(artifact)
    ).also { inMemoryCache.explodedJars(zip.name, it) }
  }

  private fun findAndroidLinter(physicalArtifact: PhysicalArtifact): String? {
    return androidLinters.find { it.coordinates == physicalArtifact.coordinates }?.lintRegistry
  }
}
