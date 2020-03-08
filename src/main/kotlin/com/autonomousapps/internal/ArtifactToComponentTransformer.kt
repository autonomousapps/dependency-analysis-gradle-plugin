package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
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
internal class ArtifactToComponentTransformer(
    private val configuration: Configuration,
    private val allArtifacts: List<Artifact>,
    private val logger: Logger
) {

    fun components(): List<Component> {
        computeTransitivity()
        return allArtifacts.asComponents()
    }

    private fun computeTransitivity() {
        val directDependencies = configuration.directDependencies()

        // "All artifacts" is everything used to compile the project. If there is a direct artifact with a matching
        // identifier, then that artifact is NOT transitive. Otherwise, it IS transitive.
        allArtifacts.forEach { artifact ->
            artifact.isTransitive = directDependencies.none { it == artifact.dependency }
        }
    }

    /**
     * Maps collection of [Artifact]s to [Component]s, basically by exploding the contents of [Artifact.file] into a set
     * of class names ([Component.classes]).
     */
    private fun Iterable<Artifact>.asComponents(): List<Component> =
        map { artifact ->
            val classes = extractClassesFromJar(artifact)
            Component(artifact.dependency, artifact.isTransitive!!, classes)
        }.sorted()

    /**
     * Analyzes bytecode (using ASM) in order to extract class names from jar ([Artifact.file]).
     */
    private fun extractClassesFromJar(artifact: Artifact): Set<String> {
        val zip = ZipFile(artifact.file)

        return zip.entries().toList()
            .filterNot { it.isDirectory }
            .filter { it.name.endsWith(".class") }
            .map { classEntry ->
                ClassNameCollector(logger).apply {
                    val reader = zip.getInputStream(classEntry).use { ClassReader(it.readBytes()) }
                    reader.accept(this, 0)
                }
            }
            .mapNotNull { it.className }
            .filterNot {
                // Filter out `java` packages, but not `javax`
                it.startsWith("java/")
            }
            .map { it.replace("/", ".") }
            .toSortedSet()
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
    .map { result ->
        val componentResult = result.selected

        when (val componentIdentifier = componentResult.id) {
            is ProjectComponentIdentifier -> Dependency(componentIdentifier)
            is ModuleComponentIdentifier -> Dependency(componentIdentifier)
            else -> throw GradleException("Unexpected ComponentIdentifier type: ${componentIdentifier.javaClass.simpleName}")
        }
    }.toSet()
