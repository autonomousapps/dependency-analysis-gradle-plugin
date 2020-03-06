@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class AbiAnalysisAggregateReportTask : DefaultTask() {

    init {
        group = TASK_GROUP_DEP
        description = "Aggregates ABI analysis reports across all subprojects"
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    lateinit var abiReports: Configuration

    @get:OutputFile
    abstract val projectReport: RegularFileProperty

    @get:OutputFile
    abstract val projectReportPretty: RegularFileProperty

    @TaskAction
    fun action() {
        // Outputs
        val projectReportFile = projectReport.get().asFile
        val projectReportPrettyFile = projectReportPretty.get().asFile
        // Cleanup prior execution
        projectReportFile.delete()
        projectReportPrettyFile.delete()

        val abiAnalysisReports = abiReports.dependencies.map { dependency ->
            val path = (dependency as ProjectDependency).dependencyProject.path

            val abiList = abiReports.fileCollection(dependency).singleFile.readLines()

            path to abiList
        }.toMap()

        projectReportFile.writeText(abiAnalysisReports.toJson())
        projectReportPrettyFile.writeText(abiAnalysisReports.toPrettyString())

        logger.debug("ABI report      : ${projectReportFile.path}")
        logger.debug("(pretty-printed): ${projectReportPrettyFile.path}")
    }
}
