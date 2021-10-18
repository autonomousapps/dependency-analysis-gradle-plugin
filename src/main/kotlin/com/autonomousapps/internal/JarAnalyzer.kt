package com.autonomousapps.internal

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.asClassFiles
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.logging.Logger
import java.util.zip.ZipFile

/**
 * Used by [com.autonomousapps.tasks.AnalyzeJarTask].
 */
internal class JarAnalyzer(
  private val artifacts: List<Artifact>,
  private val androidLinters: Set<AndroidLinterDependency>,
  private val logger: Logger,
  private val inMemoryCache: InMemoryCache
) {

  fun components(): List<Component> {
    return artifacts
      .filter { it.file.name.endsWith(".jar") }
      .asComponents()
  }

  /**
   * Maps collection of [Artifact]s to [Component]s, basically by exploding the contents of
   * [Artifact.file] into a set of class names ([Component.classes]).
   */
  private fun Iterable<Artifact>.asComponents(): List<Component> =
    map { artifact ->
      val analyzedJar = analyzeJar(artifact)
      Component(
        artifact = artifact,
        analyzedJar = analyzedJar
      )
    }.sorted()

  /**
   * Analyzes bytecode in order to extract class names and some basic structural information from
   * the jar ([Artifact.file]).
   */
  private fun analyzeJar(artifact: Artifact): AnalyzedJar {
    val zip = ZipFile(artifact.file)

    val alreadyAnalyzedJar: AnalyzedJar? = inMemoryCache.analyzedJar(zip.name)
    if (alreadyAnalyzedJar != null) {
      return alreadyAnalyzedJar
    }

    inMemoryCache.updateJars(zip.name)

    val ktFiles = KtFile.fromZip(zip)
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

    return AnalyzedJar(
      analyzedClasses = analyzedClasses,
      ktFiles = ktFiles,
      androidLintRegistry = artifact.dependency.findLinter(androidLinters)
    ).also { inMemoryCache.analyzedJars(zip.name, it) }
  }
}

/**
 * Finds the Android lint registry associated with [this][Dependency], if there is one. May return
 * null.
 */
private fun Dependency.findLinter(androidLinters: Set<AndroidLinterDependency>): String? =
  androidLinters.find { it.dependency == this }?.lintRegistry
