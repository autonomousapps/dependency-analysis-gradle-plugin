@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Produces a report of all the artifacts depended-on by the given project.
 */
open class ArtifactsAnalysisTask @Inject constructor(
    objects: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    init {
        group = "verification"
        description = "Produces a report of all classes referenced by a given jar"
    }

    /**
     * This is the "official" input for wiring task dependencies correctly, but is otherwise
     * unused.
     */
    @get:InputFiles
    lateinit var artifactFiles: FileCollection

    /**
     * This is what the task actually uses as its input. We need both the files and the artifact
     * metadata.
     */
    @get:Internal
    lateinit var resolvedArtifacts: Set<ResolvedArtifactResult>

    @get:OutputFile
    val output: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        val reportFile = output.get().asFile

        // Cleanup prior execution
        reportFile.delete()

        val artifacts = resolvedArtifacts.map {
            Artifact(
                componentIdentifier = it.id.componentIdentifier,
                file = it.file
            )
        }

        reportFile.writeText(artifacts.toJson())
    }
}
