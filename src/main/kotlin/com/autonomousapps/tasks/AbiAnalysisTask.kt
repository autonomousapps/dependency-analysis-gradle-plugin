@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.*
import com.autonomousapps.internal.kotlin.abiDependencies
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
open class AbiAnalysisTask @Inject constructor(
    objects: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    init {
        group = "verification"
        description = "Produces a report of the ABI of this project"
    }

    @get:Classpath
    val jar: RegularFileProperty = objects.fileProperty()

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val dependencies: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val output: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val abiDump: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        workerExecutor.noIsolation().submit(AbiAnalysisWorkAction::class.java) {
            jar.set(this@AbiAnalysisTask.jar)
            dependencies.set(this@AbiAnalysisTask.dependencies)
            output.set(this@AbiAnalysisTask.output)
            abiDump.set(this@AbiAnalysisTask.abiDump)
        }
    }
}

interface AbiAnalysisParameters : WorkParameters {
    val jar: RegularFileProperty
    val dependencies: RegularFileProperty
    val output: RegularFileProperty
    val abiDump: RegularFileProperty
}

abstract class AbiAnalysisWorkAction : WorkAction<AbiAnalysisParameters> {

    private val logger = getLogger<AbiAnalysisTask>()

    override fun execute() {
        // Inputs
        val jarFile = parameters.jar.get().asFile
        val dependencies = parameters.dependencies.get().asFile.readText().fromJsonList<Component>()

        // Outputs
        val reportFile = parameters.output.get().asFile
        val abiDumpFile = parameters.abiDump.get().asFile

        // Cleanup prior execution
        reportFile.delete()
        abiDumpFile.delete()

        val apiDependencies = abiDependencies(jarFile, dependencies, abiDumpFile)

        reportFile.writeText(apiDependencies.toJson())

        logger.quiet("Your full API report is at ${reportFile.path}")
        logger.quiet(
            "These are your API dependencies (see the report for more detail):\n${apiDependencies.joinToString(
                prefix = "- ",
                separator = "\n- "
            ) { lineItem(it) }}"
        )
    }

    private fun lineItem(dependency: Dependency): String {
        val advice = if (dependency.configurationName != null) {
            "(is ${dependency.configurationName})"
        } else {
            "(is transitive or unknown)"
        }

        return "${dependency.identifier} $advice"
    }
}


