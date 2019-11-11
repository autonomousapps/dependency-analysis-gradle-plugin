@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import org.objectweb.asm.ClassReader
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * This task generates a report of all dependencies, whether or not they're transitive, and the
 * classes they contain.
 */
@CacheableTask
open class DependencyReportTask @Inject constructor(
    objects: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    init {
        group = "verification"
        description = "Produces a report of all direct and transitive dependencies"
    }

    @get:Input
    val variantName: Property<String> = objects.property(String::class.java)

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
        val conf = project.configurations.getByName("${variantName.get()}RuntimeClasspath")
        val result: ResolutionResult = conf.incoming.resolutionResult
        val root: ResolvedComponentResult = result.root
        val dependencies: Set<DependencyResult> = root.dependencies

        val directArtifacts = traverseDependencies(dependencies)

        allArtifacts.forEach { dep ->
            dep.apply {
                isTransitive = !directArtifacts.any { it.identifier == dep.identifier }
            }
        }

        // Step 2. Extract declared classes from each jar
        val libraries = allArtifacts.filter {
            if (!it.file!!.exists()) {
                logger.error("File doesn't exist for dep $it")
            }
            it.file!!.exists()
        }.map { dep ->
            val z = ZipFile(dep.file)

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

            Component(dep.identifier, dep.isTransitive!!, classes)
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

