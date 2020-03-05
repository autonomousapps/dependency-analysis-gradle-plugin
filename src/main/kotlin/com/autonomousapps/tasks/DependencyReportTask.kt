@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * This task generates a report of all dependencies, whether or not they're transitive, and the
 * classes they contain. Currently uses `${variant}RuntimeClasspath`, which has visibility into all dependencies,
 * including transitive (and including those 'hidden' by `implementation`), as well as `runtimeOnly`.
 * TODO this is perhaps wrong/unnecessary. See TODO below.
 */
@CacheableTask
abstract class DependencyReportTask : DefaultTask() {

    init {
        group = TASK_GROUP_DEP
        description = "Produces a report of all direct and transitive dependencies"
    }

    /**
     * This is the "official" input for wiring task dependencies correctly, but is otherwise
     * unused.
     */
    @get:Classpath
    abstract val artifactFiles: ConfigurableFileCollection

    /**
     * This is what the task actually uses as its input. I really only care about the [ResolutionResult].
     */
    @get:Internal
    lateinit var configuration: Configuration

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val allArtifacts: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:OutputFile
    abstract val outputPretty: RegularFileProperty

    @TaskAction
    fun action() {
        // Inputs
        // This includes both direct and transitive dependencies, hence "all"
        val allArtifacts = allArtifacts.get().asFile.readText().fromJsonList<Artifact>()

        // Outputs
        val outputFile = output.get().asFile
        val outputPrettyFile = outputPretty.get().asFile
        // Cleanup prior execution
        outputFile.delete()
        outputPrettyFile.delete()

        // Actual work
        val components = ArtifactToComponentTransformer(
            // TODO I suspect I don't need to use the runtimeClasspath for getting this set of "direct artifacts"
            configuration,
            allArtifacts,
            logger
        ).components()

        // Write output to disk
        outputFile.writeText(components.toJson())
        outputPrettyFile.writeText(components.toPrettyString())
    }
}
