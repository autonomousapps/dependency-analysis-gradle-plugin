@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.*
import com.autonomousapps.internal.asm.ClassReader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * This task generates a report of all dependencies, whether or not they're transitive, and the
 * classes they contain. Current uses ${variant}RuntimeClasspath, which has visibility into all dependencies, including
 * transitive (and including those 'hidden' by `implementation`), as well as runtimeOnly.
 * TODO this is perhaps wrong/unnecessary. See TODO below
 */
@CacheableTask
open class DependencyReportTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    init {
        group = "verification"
        description = "Produces a report of all direct and transitive dependencies"
    }

    /**
     * This is the "official" input for wiring task dependencies correctly, but is otherwise
     * unused.
     */
    @get:Classpath
    lateinit var artifactFiles: FileCollection

    /**
     * This is what the task actually uses as its input. I really only care about the [ResolutionResult].
     */
    @get:Internal
    val configurationName: Property<String> = objects.property(String::class.java)

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val allArtifacts: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val output: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val outputPretty: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        // Inputs
        val allArtifacts = allArtifacts.get().asFile.readText().fromJsonList<Artifact>()

        // Outputs
        val outputFile = output.get().asFile
        val outputPrettyFile = outputPretty.get().asFile

        // Cleanup prior execution
        outputFile.delete()
        outputPrettyFile.delete()

        // Step 1. Update all-artifacts list: transitive or not?
        // runtime classpath will give me only the direct dependencies
        val dependencies: Set<DependencyResult> = project.configurations.getByName(configurationName.get())
            .incoming
            .resolutionResult
            .root
            .dependencies

        // TODO I suspect I don't need to use the runtimeClasspath for getting this set of "direct artifacts"
        val directArtifacts = traverseDependencies(dependencies)

        // "All artifacts" is everything used to compile the project. If there is a direct artifact with a matching
        // identifier, then that artifact is NOT transitive. Otherwise, it IS transitive.
        allArtifacts.forEach { dep ->
            dep.isTransitive = !directArtifacts.any { it.dependency.identifier == dep.dependency.identifier }
        }

        //printDependencyTree(dependencies)

        // TODO extract this to a testable function
        // Step 2. Extract declared classes from each jar
        val libraries = allArtifacts.filter { artifact ->
            if (!artifact.file!!.exists()) {
                throw GradleException("File doesn't exist for artifact $artifact")
            }
            artifact.file!!.exists()
        }.map { artifact ->
            val z = ZipFile(artifact.file)

            val classes = z.entries().toList()
                .filterNot { it.isDirectory }
                .filter { it.name.endsWith(".class") }
                .map { classEntry ->
                    val reader = ClassReader(z.getInputStream(classEntry).readBytes())

                    val classNameCollector = ClassNameCollector(logger)
                    reader.accept(classNameCollector, 0)
                    classNameCollector
                }
                .mapNotNull { it.className }
                .filterNot {
                    // Filter out `java` packages, but not `javax`
                    it.startsWith("java/")
                }
                .map { it.replace("/", ".") }
                .toSortedSet()

            Component(artifact.dependency, artifact.isTransitive!!, classes)
        }.sorted()

        outputFile.writeText(libraries.toJson())
        outputPrettyFile.writeText(libraries.toPrettyString())
    }
}

private fun traverseDependencies(results: Set<DependencyResult>): Set<Artifact> = results
    .filterIsInstance<ResolvedDependencyResult>()
    .map { result ->
        val componentResult = result.selected

        when (val componentIdentifier = componentResult.id) {
            is ProjectComponentIdentifier -> Artifact(componentIdentifier)
            is ModuleComponentIdentifier -> Artifact(componentIdentifier)
            else -> throw GradleException("Unexpected ComponentIdentifier type: ${componentIdentifier.javaClass.simpleName}")
        }
    }.toSet()

// Print dependency tree (like running the `dependencies` task)
fun printDependencyTree(dependencies: Set<DependencyResult>, level: Int = 0) {
    dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { result ->
        val resolvedComponentResult = result.selected
        println("${"  ".repeat(level)}- ${resolvedComponentResult.id}")
        printDependencyTree(resolvedComponentResult.dependencies, level + 1)
    }
}
