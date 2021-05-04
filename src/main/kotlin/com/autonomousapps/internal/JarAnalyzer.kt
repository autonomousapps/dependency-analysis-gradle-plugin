package com.autonomousapps.internal

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.*
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.logging.Logger
import java.util.zip.ZipException
import java.util.zip.ZipFile

/**
 * Used by [com.autonomousapps.tasks.AnalyzeJarTask].
 */
internal class JarAnalyzer(
  private val configuration: Configuration,
  private val artifacts: List<Artifact>,
  private val androidLinters: Set<AndroidLinterDependency>,
  private val logger: Logger,
  private val inMemoryCache: InMemoryCache
) {

  fun components(): List<Component> {
    computeTransitivity()
    return artifacts
      .filter { it.file.name.endsWith(".jar") }
      .asComponents()
  }

  private fun computeTransitivity() {
    val directDependencies = configuration.directDependencies()

    // "Artifacts" are everything used to compile the project. If there is a direct artifact with a
    // matching identifier, then that artifact is NOT transitive. Otherwise, it IS transitive.
    artifacts.forEach { artifact ->
      artifact.isTransitive = directDependencies.none { it == artifact.dependency }
    }
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
    // quick and drity way to log exception (instead of wrapping whole body
    try {

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
    } catch (ze: ZipException) {
      print("Failed for : " + artifact.file)
      throw ze
    }
  }
}

/**
 * Finds the Android lint registry associated with [this][Dependency], if there is one. May return
 * null.
 */
private fun Dependency.findLinter(androidLinters: Set<AndroidLinterDependency>): String? =
  androidLinters.find {
    it.dependency == this
  }?.lintRegistry

/**
 * Traverses the top level of the dependency graph to get all "direct" dependencies.
 */
private fun Configuration.directDependencies(): Set<Dependency> {
  // the only way to get flat jar file dependencies
  val fileDependencies = allDependencies
    .filterIsInstance<FileCollectionDependency>()
    .mapNotNullToSet { it.toIdentifier() }
    .mapToSet { Dependency(identifier = it, configurationName = name) }

  // Update all-artifacts list: transitive or not?
  // runtime classpath will give me only the direct dependencies
  val dependencies: Set<DependencyResult> = incoming
    .resolutionResult
    .root
    .dependencies

  return traverseDependencies(dependencies) + fileDependencies
}

/**
 * This was heavily modified from code found in the Gradle 5.6.x documentation. Can't find the link any more.
 */
private fun traverseDependencies(results: Set<DependencyResult>): Set<Dependency> = results
  .filterIsInstance<ResolvedDependencyResult>()
  .filterNot { it.isConstraint }
  .mapToSet { dependencyResult ->
    val componentResult = dependencyResult.selected

    when (val componentIdentifier = componentResult.id) {
      is ProjectComponentIdentifier -> Dependency(componentIdentifier)
      is ModuleComponentIdentifier -> Dependency(componentIdentifier)
      else -> throw GradleException("Unexpected ComponentIdentifier type: ${componentIdentifier.javaClass.simpleName}")
    }
  }
