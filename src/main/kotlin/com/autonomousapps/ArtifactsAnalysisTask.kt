@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
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
    lateinit var artifacts: ArtifactCollection

    @get:OutputFile
    val output: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val outputPretty: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        val reportFile = output.get().asFile
        val reportPrettyFile = outputPretty.get().asFile

        // Cleanup prior execution
        reportFile.delete()
        reportPrettyFile.delete()

        val artifacts = artifacts.mapNotNull {
            try {
                Artifact(
                    componentIdentifier = it.id.componentIdentifier,
                    file = it.file
                )
            } catch (e: GradleException) {
                null
            }
        }

        reportFile.writeText(artifacts.toJson())
        reportPrettyFile.writeText(artifacts.toPrettyString())
    }
}
