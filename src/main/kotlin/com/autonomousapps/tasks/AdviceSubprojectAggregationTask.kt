package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Fail
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class AdviceSubprojectAggregationTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Aggregates advice from a project's variant-specific advice tasks"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyAdvice: ListProperty<RegularFileProperty>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val redundantJvmAdvice: ListProperty<RegularFileProperty>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val redundantKaptAdvice: ListProperty<RegularFileProperty>

  /*
   * Severity
   */

  @get:Input
  abstract val failOnAny: Property<Behavior>

  @get:Input
  abstract val failOnUnusedDependencies: Property<Behavior>

  @get:Input
  abstract val failOnUsedTransitiveDependencies: Property<Behavior>

  @get:Input
  abstract val failOnIncorrectConfiguration: Property<Behavior>

  @get:Input
  abstract val failOnCompileOnly: Property<Behavior>

  @get:Input
  abstract val failOnUnusedProcs: Property<Behavior>

  @get:Input
  abstract val failOnRedundantPlugins: Property<Behavior>

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
    val dependencyAdvice: Set<Advice> = dependencyAdvice.get().flatMapToSet { it.fromJsonSet() }
    val pluginAdvice: Set<PluginAdvice> =
      redundantJvmAdvice.toPluginAdvice() + redundantKaptAdvice.toPluginAdvice()

    val shouldFailDeps = shouldFailDeps(dependencyAdvice)
    val shouldFailPlugins = shouldFailPlugins(pluginAdvice)

    // Combine
    val comprehensiveAdvice = ComprehensiveAdvice(
      dependencyAdvice = dependencyAdvice,
      pluginAdvice = pluginAdvice,
      shouldFail = shouldFailDeps || shouldFailPlugins
    )

    outputFile.writeText(comprehensiveAdvice.toJson())
    outputPrettyFile.writeText(comprehensiveAdvice.toPrettyString())
  }

  private fun shouldFailDeps(advice: Set<Advice>): Boolean {
    return failOnAny.isFail() && advice.isNotEmpty() ||
      failOnUnusedDependencies.isFail() && advice.any { it.isRemove() } ||
      failOnUsedTransitiveDependencies.isFail() && advice.any { it.isAdd() } ||
      failOnIncorrectConfiguration.isFail() && advice.any { it.isChange() } ||
      failOnCompileOnly.isFail() && advice.any { it.isCompileOnly() } ||
      failOnUnusedProcs.isFail() && advice.any { it.isProcessor() }
  }

  private fun shouldFailPlugins(pluginAdvice: Set<PluginAdvice>): Boolean {
    return failOnRedundantPlugins.isFail() && pluginAdvice.isNotEmpty()
  }

  private fun Property<Behavior>.isFail(): Boolean = get() is Fail

  private fun ListProperty<RegularFileProperty>.toPluginAdvice(): Set<PluginAdvice> =
    get().flatMapToSet {
      val file = it.get().asFile
      if (file.exists()) {
        file.readText().fromJsonSet()
      } else {
        emptySet()
      }
    }
}
