@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
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

    // TODO when this is declared as an input, Gradle shits the bed
//    @get:InputFiles
    @get:Internal
    lateinit var classpath: Configuration

    @get:OutputFile
    val output: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        val reportFile = output.get().asFile

        // Cleanup prior execution
        reportFile.delete()

        val artifacts = classpath.incoming.artifactView {
            attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes")
        }.artifacts.artifacts.map {
            Artifact(
                componentIdentifier = it.id.componentIdentifier,
                file = it.file
            )
        }

        reportFile.writeText(artifacts.toJson())
    }
}
