package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Ignore
import com.autonomousapps.internal.advice.SeverityHandler
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class FilterAdviceTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Filter merged advice based on user preferences"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val projectAdvice: RegularFileProperty

  @get:Input
  abstract val dataBindingEnabled: Property<Boolean>

  @get:Input
  abstract val viewBindingEnabled: Property<Boolean>

  @get:Input
  abstract val anyBehavior: Property<Behavior>

  @get:Input
  abstract val unusedDependenciesBehavior: Property<Behavior>

  @get:Input
  abstract val usedTransitiveDependenciesBehavior: Property<Behavior>

  @get:Input
  abstract val incorrectConfigurationBehavior: Property<Behavior>

  @get:Input
  abstract val unusedProcsBehavior: Property<Behavior>

  @get:Input
  abstract val compileOnlyBehavior: Property<Behavior>

  @get:Input
  abstract val runtimeOnlyBehavior: Property<Behavior>

  @get:Input
  abstract val redundantPluginsBehavior: Property<Behavior>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(FilterAdviceAction::class.java) {
      projectAdvice.set(this@FilterAdviceTask.projectAdvice)
      dataBindingEnabled.set(this@FilterAdviceTask.dataBindingEnabled)
      viewBindingEnabled.set(this@FilterAdviceTask.viewBindingEnabled)
      anyBehavior.set(this@FilterAdviceTask.anyBehavior)
      unusedDependenciesBehavior.set(this@FilterAdviceTask.unusedDependenciesBehavior)
      usedTransitiveDependenciesBehavior.set(this@FilterAdviceTask.usedTransitiveDependenciesBehavior)
      incorrectConfigurationBehavior.set(this@FilterAdviceTask.incorrectConfigurationBehavior)
      unusedProcsBehavior.set(this@FilterAdviceTask.unusedProcsBehavior)
      compileOnlyBehavior.set(this@FilterAdviceTask.compileOnlyBehavior)
      runtimeOnlyBehavior.set(this@FilterAdviceTask.runtimeOnlyBehavior)
      redundantPluginsBehavior.set(this@FilterAdviceTask.redundantPluginsBehavior)
      output.set(this@FilterAdviceTask.output)
    }
  }

  interface FilterAdviceParameters : WorkParameters {
    val projectAdvice: RegularFileProperty
    val dataBindingEnabled: Property<Boolean>
    val viewBindingEnabled: Property<Boolean>
    val anyBehavior: Property<Behavior>
    val unusedDependenciesBehavior: Property<Behavior>
    val usedTransitiveDependenciesBehavior: Property<Behavior>
    val incorrectConfigurationBehavior: Property<Behavior>
    val unusedProcsBehavior: Property<Behavior>
    val compileOnlyBehavior: Property<Behavior>
    val runtimeOnlyBehavior: Property<Behavior>
    val redundantPluginsBehavior: Property<Behavior>
    val output: RegularFileProperty
  }

  abstract class FilterAdviceAction : WorkAction<FilterAdviceParameters> {

    private val dataBindingEnabled = parameters.dataBindingEnabled.get()
    private val viewBindingEnabled = parameters.viewBindingEnabled.get()

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val anyBehavior = parameters.anyBehavior.get()
      val unusedDependenciesBehavior = parameters.unusedDependenciesBehavior.get()
      val usedTransitiveDependenciesBehavior = parameters.usedTransitiveDependenciesBehavior.get()
      val incorrectConfigurationBehavior = parameters.incorrectConfigurationBehavior.get()
      val unusedProcsBehavior = parameters.unusedProcsBehavior.get()
      val compileOnlyBehavior = parameters.compileOnlyBehavior.get()
      val runtimeOnlyBehavior = parameters.runtimeOnlyBehavior.get()
      val redundantPluginsBehavior = parameters.redundantPluginsBehavior.get()

      val projectAdvice = parameters.projectAdvice.fromJson<ProjectAdvice>()
      val dependencyAdvice: Set<Advice> = projectAdvice.dependencyAdvice.asSequence()
        .filterOf(anyBehavior) { true }
        .filterOf(unusedDependenciesBehavior) { it.isRemove() }
        .filterOf(usedTransitiveDependenciesBehavior) { it.isAdd() }
        .filterOf(incorrectConfigurationBehavior) { it.isChange() }
        .filterOf(compileOnlyBehavior) { it.isCompileOnly() }
        .filterOf(runtimeOnlyBehavior) { it.isRuntimeOnly() }
        .filterOf(unusedProcsBehavior) { it.isProcessor() }
        .filterDataBinding()
        .filterViewBinding()
        .toSortedSet()

      val pluginAdvice: Set<PluginAdvice> = projectAdvice.pluginAdvice.asSequence()
        .filterNot {
          anyBehavior is Ignore || anyBehavior.filter.contains(it.redundantPlugin)
        }
        .filterNot {
          redundantPluginsBehavior is Ignore || redundantPluginsBehavior.filter.contains(it.redundantPlugin)
        }
        .toSortedSet()

      val severityHandler = SeverityHandler(
        anyBehavior = anyBehavior,
        unusedDependenciesBehavior = unusedDependenciesBehavior,
        usedTransitiveDependenciesBehavior = usedTransitiveDependenciesBehavior,
        incorrectConfigurationBehavior = incorrectConfigurationBehavior,
        unusedProcsBehavior = unusedProcsBehavior,
        compileOnlyBehavior = compileOnlyBehavior,
        redundantPluginsBehavior = redundantPluginsBehavior,
      )
      val shouldFailDeps = severityHandler.shouldFailDeps(dependencyAdvice)
      val shouldFailPlugins = severityHandler.shouldFailPlugins(pluginAdvice)

      val filteredAdvice = ProjectAdvice(
        projectPath = projectAdvice.projectPath,
        dependencyAdvice = dependencyAdvice,
        pluginAdvice = pluginAdvice,
        shouldFail = shouldFailDeps || shouldFailPlugins
      )

      output.writeText(filteredAdvice.toJson())
    }

    private fun Sequence<Advice>.filterOf(behavior: Behavior, predicate: (Advice) -> Boolean): Sequence<Advice> {
      return filterNot { advice ->
        predicate(advice) && (
          behavior is Ignore
            || behavior.filter.contains(advice.coordinates.identifier)
            || behavior.filter.contains(advice.coordinates.gav())
          )
      }
    }

    private fun Sequence<Advice>.filterDataBinding(): Sequence<Advice> {
      return if (dataBindingEnabled) filterNot {
        databindingDependencies.contains(it.coordinates.identifier)
      }
      else this
    }

    private fun Sequence<Advice>.filterViewBinding(): Sequence<Advice> {
      return if (viewBindingEnabled) filterNot {
        viewBindingDependencies.contains(it.coordinates.identifier)
      }
      else this
    }
  }

  companion object {
    private val databindingDependencies = listOf(
      "androidx.databinding:databinding-adapters",
      "androidx.databinding:databinding-runtime",
      "androidx.databinding:databinding-common",
      "androidx.databinding:databinding-compiler",
      "androidx.databinding:databinding-ktx"
    )

    private val viewBindingDependencies = listOf(
      "androidx.databinding:viewbinding"
    )
  }
}
