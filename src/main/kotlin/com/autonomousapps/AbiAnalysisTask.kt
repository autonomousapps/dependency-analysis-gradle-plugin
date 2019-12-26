@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.kotlin.dump
import com.autonomousapps.internal.kotlin.filterOutNonPublic
import com.autonomousapps.internal.kotlin.getBinaryAPI
import com.autonomousapps.models.Component
import com.autonomousapps.models.Dependency
import com.autonomousapps.utils.DESC_REGEX
import com.autonomousapps.utils.allItems
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.jar.JarFile
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
        workerExecutor.await()

        logger.quiet("Your full API report is at ${output.get().asFile.path}")
        logger.quiet(
            "These are your API dependencies (see the report for more detail):\n${output.get().asFile.readText().fromJsonList<Dependency>().joinToString(
                prefix = "- ",
                separator = "\n- "
            ) { it.identifier }}"
        )
    }
}

interface AbiAnalysisParameters : WorkParameters {
    val jar: RegularFileProperty
    val dependencies: RegularFileProperty
    val output: RegularFileProperty
    val abiDump: RegularFileProperty
}

abstract class AbiAnalysisWorkAction : WorkAction<AbiAnalysisParameters> {

    override fun execute() {
        // Inputs
        val jarFile = parameters.jar.get().asFile
        val components = parameters.dependencies.get().asFile.readText().fromJsonList<Component>()

        // Outputs
        val reportFile = parameters.output.get().asFile
        val abiDumpFile = parameters.abiDump.get().asFile

        // Cleanup prior execution
        reportFile.delete()
        abiDumpFile.delete()

        val apiDependencies: Set<Dependency> = getBinaryAPI(JarFile(jarFile)).filterOutNonPublic()
            .also { publicApi ->
                abiDumpFile.bufferedWriter().use { publicApi.dump(it) }
            }
            .flatMap { classSignature ->
                val superTypes = classSignature.supertypes
                val memberTypes = classSignature.memberSignatures.map {
                    // descriptor, e.g. `(JLjava/lang/String;JI)Lio/reactivex/Single;`
                    // This one takes a long, a String, a long, and an int, and returns a Single
                    it.desc
                }.flatMap {
                    DESC_REGEX.findAll(it).allItems()
                }
                superTypes + memberTypes
            }.map {
                it.replace("/", ".")
            }.mapNotNull { fqcn ->
                components.find { component ->
                    component.classes.contains(fqcn)
                }?.dependency
            }.toSortedSet()

        reportFile.writeText(apiDependencies.toJson())
    }
}
