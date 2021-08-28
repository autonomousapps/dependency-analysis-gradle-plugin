package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.graph.DependencyGraph
import com.autonomousapps.internal.advice.SeverityHandler
import com.autonomousapps.internal.graph.GraphMinimizer
import com.autonomousapps.internal.graph.LazyDependencyGraph
import com.autonomousapps.internal.graph.projectGraphMapFrom
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
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

  /*
   * Severity
   */

  @get:Input
  abstract val anyBehavior: Property<Behavior>

  @get:Input
  abstract val unusedDependenciesBehavior: Property<Behavior>

  @get:Input
  abstract val usedTransitiveDependenciesBehavior: Property<Behavior>

  @get:Input
  abstract val incorrectConfigurationBehavior: Property<Behavior>

  @get:Input
  abstract val compileOnlyBehavior: Property<Behavior>

  @get:Input
  abstract val unusedProcsBehavior: Property<Behavior>

  @get:Input
  abstract val redundantPluginsBehavior: Property<Behavior>

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

    val severityHandler = SeverityHandler(
      anyBehavior = anyBehavior.get(),
      unusedDependenciesBehavior = unusedDependenciesBehavior.get(),
      usedTransitiveDependenciesBehavior = usedTransitiveDependenciesBehavior.get(),
      incorrectConfigurationBehavior = incorrectConfigurationBehavior.get(),
      compileOnlyBehavior = compileOnlyBehavior.get(),
      unusedProcsBehavior = unusedProcsBehavior.get(),
      redundantPluginsBehavior = redundantPluginsBehavior.get()
    )
    val dependencyAdvice = mutableSetOf<Advice>()
    val pluginAdvice = mutableSetOf<PluginAdvice>()
    minimalAdvice.forEach {
      dependencyAdvice.addAll(it.dependencyAdvice)
      pluginAdvice.addAll(it.pluginAdvice)
    }
    val shouldFailDeps = severityHandler.shouldFailDeps(dependencyAdvice)
    val shouldFailPlugins = severityHandler.shouldFailPlugins(pluginAdvice)

    // Kludge: we set all projects' advice to "fail" if SeverityHandler says we should globally fail
    // This value is ultimately read by BuildHealthTask
    val writableAdvice = minimalAdvice.map {
      it.copy(shouldFail = shouldFailDeps || shouldFailPlugins)
    }

    outputFile.writeText(writableAdvice.toJson())
    outputPrettyFile.writeText(writableAdvice.toPrettyString())
  }

  private val lazyDepGraph by lazy {
    LazyDependencyGraph(projectGraphMapFrom(graphs))
  }

  private fun getDependencyGraph(projectPath: String): DependencyGraph? {
    return lazyDepGraph.getDependencyGraphOrNull(projectPath)
  }
}
