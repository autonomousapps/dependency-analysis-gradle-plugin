@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.ClocReport
import com.autonomousapps.internal.ClocSummaryReport
import com.autonomousapps.internal.fromJsonList
import com.autonomousapps.internal.toCsv
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Summarizes reports across an arbitrary number of subprojects.
 */
@CacheableTask
open class ClocSummaryTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {

    @OutputFile
    val summaryReport: RegularFileProperty = objects.fileProperty()

    @OutputFile
    val summaryReportCsv: RegularFileProperty = objects.fileProperty()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    val inputReports: ListProperty<RegularFile> = objects.listProperty(RegularFile::class.java)

    @TaskAction
    fun action() {
        val summaryReportFile = summaryReport.get().asFile
        val summaryReportCsvFile = summaryReportCsv.get().asFile

        // Cleanup prior execution
        summaryReportFile.delete()
        summaryReportCsvFile.delete()

        val clocReports = inputReports.get()
            .asSequence()
            .map { it.asFile }
            .filter { it.exists() }
            .map { it.readText().fromJsonList<ClocReport>() }
            .filter { it.isNotEmpty() }
            .map {
                ClocSummaryReport(
                    projectName = it.first().projectName,
                    clocReports = it
                )
            }
            .sortedBy { it.projectName }
            .toList()

        summaryReportFile.writeText(clocReports.toJson())
        logger.quiet("Report:\n${clocReports.toPrettyString()}")

        summaryReportCsvFile.writeText(clocReports.toCsv())
        logger.quiet("Report:\n${summaryReportCsvFile.readText()}")
    }
}
