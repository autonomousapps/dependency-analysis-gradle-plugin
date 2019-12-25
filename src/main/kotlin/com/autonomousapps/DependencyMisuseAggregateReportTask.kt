@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.UnusedDirectComponent
import com.autonomousapps.internal.fromJsonList
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import javax.inject.Inject

@CacheableTask
open class DependencyMisuseAggregateReportTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {

    init {
        group = "verification"
    }

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    lateinit var unusedDependencyReports: Configuration

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

        val unusedDirectDependencies = unusedDependencyReports.dependencies.map { dependency ->
            val path = (dependency as ProjectDependency).dependencyProject.path

            val unusedDependencies = unusedDependencyReports.fileCollection(dependency).files
                .first()
                .readText().fromJsonList<UnusedDirectComponent>()

            path to unusedDependencies
        }.toMap()


        // TODO currently unused. Will be part of HTMl report at least
//        val usedTransitiveDependencies = projectReportTasks.map {
//            it.project.name to it.outputUsedTransitives.get().asFile
//        }.map { nameToFile ->
//            nameToFile.first to nameToFile.second.readText().fromJsonList<TransitiveDependency>()
//        }.toMap()

        projectReportFile.writeText(unusedDirectDependencies.toJson())
        projectReportPrettyFile.writeText(unusedDirectDependencies.toPrettyString())

        logger.quiet("Unused dependencies report: ${projectReportFile.path}")
        logger.quiet("Unused dependencies report, pretty-printed: ${projectReportPrettyFile.path}")

        // TODO write an HTML report
    }
}
