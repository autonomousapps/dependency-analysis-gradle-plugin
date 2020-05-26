package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
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

    // Combine
    val comprehensiveAdvice = ComprehensiveAdvice(dependencyAdvice, pluginAdvice)

    outputFile.writeText(comprehensiveAdvice.toJson())
    outputPrettyFile.writeText(comprehensiveAdvice.toPrettyString())
  }

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
