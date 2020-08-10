package com.autonomousapps.internal

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.asClassFiles
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.logging.Logger
import java.util.zip.ZipFile

/**
 * Used by [DependencyReportTask][com.autonomousapps.tasks.DependencyReportTask].
 */
internal class JarAnalyzer(
  private val configuration: Configuration,
  private val artifacts: List<Artifact>,
  private val logger: Logger,
  private val inMemoryCache: InMemoryCache
) {

  fun components(): List<Component> {
    computeTransitivity()
    return artifacts.asComponents()
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
      Component(artifact = artifact, analyzedJar = analyzedJar)
    }.sorted()

  /**
   * Analyzes bytecode in order to extract class names and some basic structural information from
   * the jar ([Artifact.file]).
   */
  private fun analyzeJar(artifact: Artifact): AnalyzedJar {
    val zip = ZipFile(artifact.file)

    val alreadyAnalyzedJar: AnalyzedJar? = inMemoryCache.analyzedJars[zip.name]
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

    return AnalyzedJar(analyzedClasses, ktFiles).also {
      inMemoryCache.analyzedJars.putIfAbsent(zip.name, it)
    }
  }
}

/**
 * Traverses the top level of the dependency graph to get all "direct" dependencies.
 */
private fun Configuration.directDependencies(): Set<Dependency> {
  // Update all-artifacts list: transitive or not?
  // runtime classpath will give me only the direct dependencies
  val dependencies: Set<DependencyResult> =
    incoming
      .resolutionResult
      .root
      .dependencies

  return traverseDependencies(dependencies)
}

/**
 * This was heavily modified from code found in the Gradle 5.6.x documentation. Can't find the link any more.
 */
private fun traverseDependencies(results: Set<DependencyResult>): Set<Dependency> = results
  .filterIsInstance<ResolvedDependencyResult>()
  .mapToSet { result ->
    val componentResult = result.selected

    when (val componentIdentifier = componentResult.id) {
      is ProjectComponentIdentifier -> Dependency(componentIdentifier)
      is ModuleComponentIdentifier -> Dependency(componentIdentifier)
      else -> throw GradleException("Unexpected ComponentIdentifier type: ${componentIdentifier.javaClass.simpleName}")
    }
  }
