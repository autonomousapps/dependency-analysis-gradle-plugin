@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.Callable
import javax.inject.Inject

open class AbiAnalysisAggregateReportTask @Inject constructor(
    objects: ObjectFactory
): DefaultTask() {

    // TODO I think Gradle 6 permits Kotlin higher-order functions
    @get:Internal
    lateinit var projectReportCallables: Callable<List<AbiAnalysisTask>>

    @get:OutputFile
    val projectReport: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val projectReportPretty: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        // Outputs
        val projectReportFile = projectReport.get().asFile
        val projectReportPrettyFile = projectReportPretty.get().asFile
        // Cleanup prior execution
        projectReportFile.delete()
        projectReportPrettyFile.delete()

        // Inputs
        val projectReportTasks = projectReportCallables.call()

        val abiReports = projectReportTasks.map {
            it.project.name to it.output.get().asFile
        }.map { nameToFile ->
            nameToFile.first to nameToFile.second.readLines()
        }.toMap()

        projectReportFile.writeText(abiReports.toJson())
        projectReportPrettyFile.writeText(abiReports.toPrettyString())

        logger.quiet("Unused dependencies report: ${projectReportFile.path}")
        logger.quiet("Unused dependencies report, pretty-printed: ${projectReportPrettyFile.path}")

        // TODO write an HTML report
    }
}
