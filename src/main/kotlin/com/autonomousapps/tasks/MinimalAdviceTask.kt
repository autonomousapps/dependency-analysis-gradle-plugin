package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.graph.DependencyGraph
import com.autonomousapps.internal.graph.GraphMinimizer
import com.autonomousapps.internal.graph.LazyDependencyGraph
import com.autonomousapps.internal.graph.projectGraphMapFrom
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class MinimalAdviceTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Aggregates advice reports across all subprojects, minimized"
  }

  /**
   * A `List<ComprehensiveAdvice>`.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val adviceReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var graphs: Configuration

  /**
   * Merged dependency graph.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val mergedGraph: RegularFileProperty

  /**
   * Merged dependents graph (reverse of the above).
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val mergedRevGraph: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val outputPretty: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()
    val outputPrettyFile = outputPretty.getAndDelete()

    val buildHealth = adviceReport.fromJsonList<ComprehensiveAdvice>()

    val minimalAdvice = GraphMinimizer(
      buildHealth = buildHealth,
      dependentsGraph = mergedRevGraph.fromJson(),
      lazyDepGraph = this::getDependencyGraph
    ).minimalBuildHealth

    outputFile.writeText(minimalAdvice.toJson())
    outputPrettyFile.writeText(minimalAdvice.toPrettyString())
  }

  private val lazyDepGraph by lazy {
    LazyDependencyGraph(projectGraphMapFrom(graphs))
  }

  private fun getDependencyGraph(projectPath: String): DependencyGraph {
    return lazyDepGraph.getDependencyGraph(projectPath)
  }
}
