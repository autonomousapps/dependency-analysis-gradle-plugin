package com.autonomousapps.internal

internal data class ClocReport(
    val projectName: String,
    val sourceType: String,
    val fileCount: Int,
    val lineCount: Int
) {
    operator fun plus(other: ClocReport) = ClocReport(
        projectName, sourceType,
        fileCount + other.fileCount,
        lineCount + other.lineCount
    )
}

internal data class ClocSummaryReport(
    val projectName: String,
    val clocReports: List<ClocReport>
)
