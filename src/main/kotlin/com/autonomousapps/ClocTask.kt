@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.ClocReport
import com.autonomousapps.internal.JvmLineCounter
import com.autonomousapps.internal.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * Calculates lines of code for a particular subproject and a specific source type (such as Java or
 * Kotlin).
 */
@CacheableTask
open class ClocTask @Inject constructor(
    objects: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    @OutputFile
    val report: RegularFileProperty = objects.fileProperty()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    lateinit var source: FileTree

    @Input
    val type: Property<String> = objects.property(String::class.java)

    @TaskAction
    fun action() {
        val reportFile = report.get().asFile

        // Cleanup prior execution
        reportFile.delete()

        workerExecutor.noIsolation().submit(ClocWorkAction::class.java) { params ->
            with(params) {
                source = this@ClocTask.source.files
                projectName.set(project.name)
                sourceType.set(type.get())
                report.set(reportFile)
            }
        }
        workerExecutor.await()

        logger.quiet("Report:\n${reportFile.readText()}")
    }
}

interface ClocParameters : WorkParameters {
    var source: Set<File>
    val projectName: Property<String>
    val sourceType: Property<String>
    val report: RegularFileProperty
}

abstract class ClocWorkAction : WorkAction<ClocParameters> {

    private val lineCounter = JvmLineCounter

    override fun execute() {
        // TODO convert to coroutines?
        val fileCount = parameters.source.filter { it.isFile }.count()
        val lineCount = parameters.source.parallelStream()
            .filter { it.isFile }
            .map { lineCounter.countLines(it) }
            .reduce { t, u -> t + u }
            .orElseGet { 0 }

        val clocReport = ClocReport(
            projectName = parameters.projectName.get(),
            sourceType = parameters.sourceType.get(),
            fileCount = fileCount,
            lineCount = lineCount
        )
        parameters.report.get().asFile.writeText(clocReport.toJson())
    }
}
