package com.autonomousapps.tasks

import com.autonomousapps.FLAG_SILENT
import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.*
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Fail
import com.autonomousapps.internal.utils.*
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
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

  /*
   * Caches.
   */

  @get:Internal
  abstract val inMemoryCacheProvider: Property<InMemoryCache>

  @TaskAction fun action() {
    // Outputs
    val outputFile = output.getAndDelete()
    val outputPrettyFile = outputPretty.getAndDelete()

    // Inputs
    val dependencyAdvice: Set<Advice> = dependencyAdvice.get().flatMapToOrderedSet { it.fromJsonSet() }
    val pluginAdvice: Set<PluginAdvice> = redundantJvmAdvice.toPluginAdvice() + redundantKaptAdvice.toPluginAdvice()

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
      dependencyAdvice = dependencyAdvice,
      pluginAdvice = pluginAdvice,
      shouldFail = shouldFailDeps || shouldFailPlugins
    )

    printToConsole(comprehensiveAdvice)
    if (shouldNotBeSilent()) {
      logger.quiet("\nSee machine-readable report at ${outputFile.path}")
      logger.quiet("See pretty report at           ${outputPrettyFile.path}")
    }

    outputFile.writeText(comprehensiveAdvice.toJson())
    outputPrettyFile.writeText(comprehensiveAdvice.toPrettyString())

    if (shouldFailDeps) {
      inMemoryCacheProvider.get().error(AdviceException("Task $path failed due to misused dependencies"))
    }
    if (shouldFailPlugins) {
      inMemoryCacheProvider.get().error(AdviceException("Task $path failed due to misused plugins"))
    }
  }

  private fun ListProperty<RegularFile>.toPluginAdvice(): Set<PluginAdvice> =
    get().flatMapToSet {
      val file = it.asFile
      if (file.exists()) {
        file.readText().fromJsonSet()
      } else {
        emptySet()
      }
    }

  private fun printToConsole(comprehensiveAdvice: ComprehensiveAdvice) {
    val builder = StringBuilder()
    with(comprehensiveAdvice.dependencyAdvice) {

      // remove advice
      with(filter { it.isRemove() }) {
        if (isNotEmpty()) {
          builder
            .append("Unused dependencies which should be removed:\n")
            .append(joinToString(separator = "\n") {
              "- ${it.fromConfiguration}(${it.dependency.printableIdentifier()})"
            })
        }
      }

      // add advice
      with(filter { it.isAdd() }) {
        if (isNotEmpty()) {
          if (builder.isNotEmpty()) {
            builder.append("\n\n")
          }
          builder
            .append("Transitively used dependencies that should be declared directly as indicated:\n")
            .append(joinToString(separator = "\n") {
              "- ${it.toConfiguration}(${it.dependency.printableIdentifier()})"
            })
        }
      }

      // change advice
      with(filter { it.isChange() }) {
        if (isNotEmpty()) {
          if (builder.isNotEmpty()) {
            builder.append("\n\n")
          }
          builder
            .append("Existing dependencies which should be modified to be as indicated:\n")
            .append(joinToString(separator = "\n") {
              "- ${it.toConfiguration}(${it.dependency.printableIdentifier()}) (was ${it.fromConfiguration})"
            })
        }
      }

      // compileOnly advice
      with(filter { it.isCompileOnly() }) {
        if (isNotEmpty()) {
          if (builder.isNotEmpty()) {
            builder.append("\n\n")
          }
          builder
            .append("Dependencies which could be compile-only:\n")
            .append(joinToString(separator = "\n") {
              "- ${it.toConfiguration}(${it.dependency.printableIdentifier()}) (was ${it.fromConfiguration})"
            })
        }
      }

      // unused processer advice
      with(filter { it.isProcessor() }) {
        if (isNotEmpty()) {
          if (builder.isNotEmpty()) {
            builder.append("\n\n")
          }
          builder
            .append("Unused annotation processors that should be removed:\n")
            .append(joinToString(separator = "\n") {
              "- ${it.fromConfiguration}(${it.dependency.printableIdentifier()})"
            })
        }
      }
    }

    with(comprehensiveAdvice.pluginAdvice) {
      if (isNotEmpty()) {
        if (builder.isNotEmpty()) {
          builder.append("\n\n")
        }
        builder
          .append("Plugins that should be removed:\n")
          .append(joinToString(separator = "\n") {
            "- ${it.redundantPlugin}, because ${it.reason}"
          })
      }
    }

    if (shouldNotBeSilent()) {
      logger.quiet(builder.toString())
    }
  }

  private fun Dependency.printableIdentifier(): String =
    if (dependency.identifier.startsWith(":")) {
      "project(\"${dependency.identifier}\")"
    } else {
      "\"${dependency.identifier}:${dependency.resolvedVersion}\""
    }
}

private fun shouldNotBeSilent(): Boolean {
  val silent = System.getProperty(FLAG_SILENT, "false")
  return !silent!!.toBoolean()
}

// TODO move
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
