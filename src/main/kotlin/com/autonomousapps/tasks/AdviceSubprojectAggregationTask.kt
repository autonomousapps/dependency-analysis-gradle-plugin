package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Fail
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class AdviceSubprojectAggregationTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Aggregates advice from a project's variant-specific advice tasks"
  }

  private val projectPath = project.path

  /*
   * Inputs
   */

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyAdvice: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val redundantJvmAdvice: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val redundantKaptAdvice: ListProperty<RegularFile>

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

  /*
   * Outputs
   */

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val outputPretty: RegularFileProperty

  @TaskAction fun action() {
    // Outputs
    val outputFile = output.getAndDelete()
    val outputPrettyFile = outputPretty.getAndDelete()

    // Inputs
    val dependencyAdvice = dependencyAdvice.get().flatMapToOrderedSet { it.fromJsonSet<Advice>() }
    val pluginAdvice = redundantJvmAdvice.toPluginAdvice() + redundantKaptAdvice.toPluginAdvice()

    val severityHandler = SeverityHandler(
      anyBehavior = anyBehavior.get(),
      unusedDependenciesBehavior = unusedDependenciesBehavior.get(),
      usedTransitiveDependenciesBehavior = usedTransitiveDependenciesBehavior.get(),
      incorrectConfigurationBehavior = incorrectConfigurationBehavior.get(),
      compileOnlyBehavior = compileOnlyBehavior.get(),
      unusedProcsBehavior = unusedProcsBehavior.get(),
      redundantPluginsBehavior = redundantPluginsBehavior.get()
    )
    val shouldFailDeps = severityHandler.shouldFailDeps(dependencyAdvice)
    val shouldFailPlugins = severityHandler.shouldFailPlugins(pluginAdvice)

    // Combine
    val comprehensiveAdvice = ComprehensiveAdvice(
      projectPath = projectPath,
      dependencyAdvice = dependencyAdvice,
      pluginAdvice = pluginAdvice,
      shouldFail = shouldFailDeps || shouldFailPlugins
    )

    outputFile.writeText(comprehensiveAdvice.toJson())
    outputPrettyFile.writeText(comprehensiveAdvice.toPrettyString())
  }

  private fun ListProperty<RegularFile>.toPluginAdvice(): Set<PluginAdvice> = get()
    .flatMapToSet {
      val file = it.asFile
      if (file.exists()) {
        file.readText().fromJsonSet()
      } else {
        emptySet()
      }
    }
}

internal class SeverityHandler(
  private val anyBehavior: Behavior,
  private val unusedDependenciesBehavior: Behavior,
  private val usedTransitiveDependenciesBehavior: Behavior,
  private val incorrectConfigurationBehavior: Behavior,
  private val compileOnlyBehavior: Behavior,
  private val unusedProcsBehavior: Behavior,
  private val redundantPluginsBehavior: Behavior
) {
  fun shouldFailDeps(advice: Set<Advice>): Boolean {
    return anyBehavior.isFail() && advice.isNotEmpty() ||
      unusedDependenciesBehavior.isFail() && advice.any { it.isRemove() } ||
      usedTransitiveDependenciesBehavior.isFail() && advice.any { it.isAdd() } ||
      incorrectConfigurationBehavior.isFail() && advice.any { it.isChange() } ||
      compileOnlyBehavior.isFail() && advice.any { it.isCompileOnly() } ||
      unusedProcsBehavior.isFail() && advice.any { it.isProcessor() }
  }

  fun shouldFailPlugins(pluginAdvice: Set<PluginAdvice>): Boolean {
    return redundantPluginsBehavior.isFail() && pluginAdvice.isNotEmpty()
  }

  private fun Behavior.isFail(): Boolean = this is Fail
}
