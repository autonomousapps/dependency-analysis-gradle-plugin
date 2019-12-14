@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.Component
import com.autonomousapps.internal.fromJsonList
import com.autonomousapps.internal.kotlin.dump
import com.autonomousapps.internal.kotlin.filterOutNonPublic
import com.autonomousapps.internal.kotlin.getBinaryAPI
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory
import java.util.jar.JarFile
import javax.inject.Inject

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

    @get:InputFile
    val dependencies: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val output: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
//        // Inputs
//        val jarFile = jar.get().asFile
//        val components = dependencies.get().asFile.readText().fromJsonList<Component>()
//
//        // Outputs
//        val reportFile = output.get().asFile
//
//        // Cleanup prior execution
//        reportFile.delete()

        workerExecutor.noIsolation().submit(AbiAnalysisWorkAction::class.java) {
            jar.set(this@AbiAnalysisTask.jar)
            dependencies.set(this@AbiAnalysisTask.dependencies)
            output.set(this@AbiAnalysisTask.output)
        }
    }
}

interface AbiAnalysisParameters : WorkParameters {
    val jar: RegularFileProperty
    val dependencies: RegularFileProperty
    val output: RegularFileProperty
}

abstract class AbiAnalysisWorkAction : WorkAction<AbiAnalysisParameters> {

    private val logger = LoggerFactory.getLogger(AbiAnalysisWorkAction::class.java)

    override fun execute() {
        // Inputs
        val jarFile = parameters.jar.get().asFile
        val components = parameters.dependencies.get().asFile.readText().fromJsonList<Component>()

        // Outputs
        val reportFile = parameters.output.get().asFile

        // Cleanup prior execution
        reportFile.delete()

        reportFile.bufferedWriter().use {
            getBinaryAPI(JarFile(jarFile)).filterOutNonPublic().dump(it)
        }
//            .map { classSignature ->
//                classSignature
//            }
    }
}
