// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.ProjectTypeUsage
import com.autonomousapps.model.internal.AggregateTypeUsageReport
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
public abstract class AggregateTypeUsageTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Computes type-level dependency usage, aggregating all variants or source sets in this project"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val typeUsageReports: ConfigurableFileCollection

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction
  public fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.typeUsageReports.setFrom(typeUsageReports)
      it.output.set(output)
    }
  }

  public interface Parameters : WorkParameters {
    public val typeUsageReports: ConfigurableFileCollection
    public val output: RegularFileProperty
  }

  public interface Action : WorkAction<Parameters> {
    override fun execute() {
      val output = parameters.output.getAndDelete()

      val reports = parameters.typeUsageReports.files.map { it.fromJson<ProjectTypeUsage>() }
      val combiner = ProjectTypeUsageCombiner(reports)
      val aggregateReport = combiner.combine()

      output.bufferWriteJson(aggregateReport)
    }
  }

  // TODO(tsr): consider unit tests for this complex logic
  // internal for testing
  internal class ProjectTypeUsageCombiner(private val reports: List<ProjectTypeUsage>) {
    fun combine(): AggregateTypeUsageReport {
      val builder = AggregateTypeUsageReport.Builder()
      reports.forEach { report ->
        with(builder) {
          projectPath = report.projectPath
          internal.addAll(report.internal.keys)
          unknownDependencies.addAll(report.unknownDependencies.keys)
          report.projectDependencies.forEach { (coords, classNamesMap) ->
            classNamesMap.keys.forEach { className ->
              projectDependencies.merge(coords, sortedSetOf(className)) { acc, inc ->
                acc.apply { addAll(inc) }
              }
            }
          }
          report.libraryDependencies.forEach { (coords, classNamesMap) ->
            classNamesMap.keys.forEach { className ->
              libraryDependencies.merge(coords, sortedSetOf(className)) { acc, inc ->
                acc.apply { addAll(inc) }
              }
            }
          }
        }
      }

      return builder.build()
    }
  }
}
