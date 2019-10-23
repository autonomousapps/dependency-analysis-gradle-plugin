package com.autonomousapps.internal

// TODO provide configuration option for sort order?
internal fun List<ClocSummaryReport>.toCsv(): String {
    val builder = StringBuilder("Project,Java Files,Kotlin Files,Sum (Files),Java LOC,Kotlin LOC,Sum (LOC),LOC Portion of Total\n")

    // Sort by total LOC descending, and then one line per project
    val csvLines = sortedBy { it.clocReports.sumBy { -it.lineCount } }
        .map { summaryReport ->
            val javaReport = summaryReport.clocReports.find { it.sourceType == "java" }
            val kotlinReport = summaryReport.clocReports.find { it.sourceType == "kotlin" }

            CsvLine(
                projectName = summaryReport.projectName,
                javaFileCount = javaReport?.fileCount ?: 0,
                kotlinFileCount = kotlinReport?.fileCount ?: 0,
                javaLoc = javaReport?.lineCount ?: 0,
                kotlinLoc = kotlinReport?.lineCount ?: 0
            )
        }

    // Because we want to include the percentage on each line
    val totalLoc = csvLines.sumBy { it.totalLoc }

    // Now we can write the lines
    csvLines.forEach {
        builder.append("${it.projectName},${it.javaFileCount},${it.kotlinFileCount},${it.totalFileCount},${it.javaLoc},${it.kotlinLoc},${it.totalLoc},${it.totalLoc.toFloat() / totalLoc}\n")
    }

    return builder.toString()
}

private data class CsvLine(
    val projectName: String,
    val javaFileCount: Int,
    val kotlinFileCount: Int,
    val javaLoc: Int,
    val kotlinLoc: Int
) {
    val totalFileCount = javaFileCount + kotlinFileCount
    val totalLoc = javaLoc + kotlinLoc
}
