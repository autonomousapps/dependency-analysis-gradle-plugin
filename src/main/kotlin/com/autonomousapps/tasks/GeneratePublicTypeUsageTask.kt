// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.internal.AggregateTypeUsageReport
import com.autonomousapps.model.internal.PublicTypeUsage
import com.autonomousapps.model.internal.PublicTypeUsage.Report
import com.autonomousapps.model.internal.PublicTypes
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.*
import javax.inject.Inject

@CacheableTask
public abstract class GeneratePublicTypeUsageTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Generates the aggregate public class usage report"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val publicClassesReports: ConfigurableFileCollection

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val typeUsageReports: ConfigurableFileCollection

  /**
   * A machine-readable JSON report.
   *
   * @see [PublicTypeUsage]
   */
  @get:OutputFile
  public abstract val output: RegularFileProperty

  /** A human-readable console report, derived from [output]. */
  @get:OutputFile
  public abstract val outputConsole: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.publicClassesReports.setFrom(publicClassesReports)
      it.typeUsageReports.setFrom(typeUsageReports)
      it.output.set(output)
      it.outputConsole.set(outputConsole)
    }
  }

  public interface Parameters : WorkParameters {
    public val publicClassesReports: ConfigurableFileCollection
    public val typeUsageReports: ConfigurableFileCollection
    public val output: RegularFileProperty
    public val outputConsole: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {
    override fun execute() {
      val output = parameters.output.getAndDelete()
      val outputConsole = parameters.outputConsole.getAndDelete()

      val publicClassReports = parameters.publicClassesReports.mapToOrderedSet { it.fromJson<PublicTypes>() }
      val typeUsageReports = parameters.typeUsageReports.mapToOrderedSet { it.fromJson<AggregateTypeUsageReport>() }
      val analyzer = Analyzer(publicClassReports, typeUsageReports)
      val publicClassUsage = analyzer.analyze()
      val consoleReport = ConsoleReportBuilder(publicClassUsage).buildReport()

      output.bufferWriteJson(publicClassUsage)
      outputConsole.writeText(consoleReport)
    }
  }

  private class Analyzer(
    private val publicClassReports: Set<PublicTypes>,
    private val typeUsageReports: Set<AggregateTypeUsageReport>,
  ) {
    fun analyze(): PublicTypeUsage {
      // owningProjectPath -> accessors (accessingProjectPath -> accessedClasses)
      val accessorMap = mutableMapOf<String, SortedSet<Accessor>>()

      typeUsageReports.forEach { report ->
        val accessingProjectPath = report.projectPath
        // only projectDependencies could be accessors of public classes in this build
        report.projectDependencies.entries.forEach { (owningProjectPath, classNames) ->
          val accessor = Accessor(accessingProjectPath, classNames)
          accessorMap.merge(owningProjectPath, sortedSetOf(accessor)) { acc, inc ->
            acc.apply { addAll(inc) }
          }
        }
      }

      // owningProject -> reports
      val reports = publicClassReports.mapToOrderedSet { publicClasses ->
        val owningProject = publicClasses.projectPath
        val builder = ReportBuilder().apply {
          this.owningProject = owningProject
          this.publicClasses.addAll(publicClasses.types)
        }

        // Can be null (empty) if there are no accessors
        val accessors = accessorMap[owningProject].orEmpty()

        accessors.forEach { accessor ->
          val accessingProject = accessor.accessingProject
          val accessedClasses = accessor.accessedClasses

          builder.accessedClasses.addAll(accessedClasses)

          accessedClasses.forEach { accessedClass ->
            builder.accesses.merge(accessedClass, sortedSetOf(accessingProject)) { acc, inc ->
              acc.apply { addAll(inc) }
            }
          }
        }

        builder.build()
      }

      return PublicTypeUsage(reports)
    }
  }

  private data class Accessor(
    val accessingProject: String,
    /** From a specific other subproject. */
    val accessedClasses: Set<String>,
  ) : Comparable<Accessor> {
    override fun compareTo(other: Accessor): Int {
      return compareBy(Accessor::accessingProject)
        .thenBy(LexicographicIterableComparator()) { it.accessedClasses }
        .compare(this, other)
    }
  }

  private class ReportBuilder {
    var owningProject: String? = null
    val publicClasses = mutableSetOf<String>()
    val accessedClasses = mutableSetOf<String>()
    val accesses = mutableMapOf<String, SortedSet<String>>()
    val unaccessedClasses = sortedSetOf<String>()

    fun build(): Report {
      return Report(
        owningProject = requireNotNull(owningProject),
        accesses = buildAccesses(),
        unaccessedTypes = (publicClasses - accessedClasses).toSortedSet(),
      )
    }

    private fun buildAccesses(): Set<PublicTypeUsage.Accesses> {
      return accesses
        .mapTo(TreeSet()) { (className, accessingProjects) ->
          PublicTypeUsage.Accesses(
            typeName = className,
            accessingProjects = accessingProjects,
          )
        }
    }
  }

  private class ConsoleReportBuilder(private val report: PublicTypeUsage) {
    fun buildReport() = buildString {
      appendLine("These projects have public types (classes, interfaces) that have no external accessors. Those types' visibilities could be restricted (e.g., made `internal`).")
      appendLine()

      var lineBreak = false

      report.reports.asSequence()
        .filter { r -> r.unaccessedTypes.isNotEmpty() }
        .forEach { r ->
          if (lineBreak) {
            appendLine()
          }
          lineBreak = true

          val size = r.unaccessedTypes.size

          append(r.owningProject)
          append(" (")
          append(size)
          append(" ")
          append(pluralize(size))
          appendLine(")")

          r.unaccessedTypes.forEach { type ->
            append("- ").appendLine(type)
          }
        }
    }

    private fun pluralize(size: Int): String = if (size == 1) {
      "type"
    } else {
      "types"
    }
  }
}
