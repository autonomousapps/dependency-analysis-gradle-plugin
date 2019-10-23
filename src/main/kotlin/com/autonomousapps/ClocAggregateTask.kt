@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.ClocReport
import com.autonomousapps.internal.fromJson
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import javax.inject.Inject

/**
 * Aggregates the various reports within a single subproject.
 */
@CacheableTask
open class ClocAggregateTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {

    @OutputFile
    val report: RegularFileProperty = objects.fileProperty()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    val inputReports: ListProperty<RegularFile> = objects.listProperty(RegularFile::class.java)

    @TaskAction
    fun action() {
        val reportFile = report.get().asFile

        // Cleanup prior execution
        reportFile.delete()

        val clocReports = inputReports.get()
            .map { it.asFile }
            .filter { it.exists() }
            .map { it.readText().fromJson<ClocReport>() }
            .filter { it.fileCount != 0 }

        // TODO this might be redundant now.
        val summaryReport = clocReports.groupBy { it.sourceType }.values.map {
            it.reduce { acc, report ->
                acc + report
            }
        }

        reportFile.writeText(summaryReport.toJson())

        logger.quiet("Report:\n${summaryReport.toPrettyString()}")
    }
}