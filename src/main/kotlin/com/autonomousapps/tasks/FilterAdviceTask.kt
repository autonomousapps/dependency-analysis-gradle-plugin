// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Ignore
import com.autonomousapps.extension.Issue
import com.autonomousapps.extension.anyMatches
import com.autonomousapps.internal.DependencyScope
import com.autonomousapps.internal.advice.SeverityHandler
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
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
  abstract val anyBehavior: ListProperty<Behavior>

  @get:Input
  abstract val unusedDependenciesBehavior: ListProperty<Behavior>

  @get:Input
  abstract val usedTransitiveDependenciesBehavior: ListProperty<Behavior>

  @get:Input
  abstract val incorrectConfigurationBehavior: ListProperty<Behavior>

  @get:Input
  abstract val unusedProcsBehavior: ListProperty<Behavior>

  @get:Input
  abstract val compileOnlyBehavior: ListProperty<Behavior>

  @get:Input
  abstract val runtimeOnlyBehavior: ListProperty<Behavior>

  @get:Input
  abstract val duplicateClassWarningsBehavior: ListProperty<Behavior>

  @get:Input
  abstract val redundantPluginsBehavior: Property<Behavior>

  @get:Input
  abstract val moduleStructureBehavior: Property<Behavior>

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
      duplicateClassWarningsBehavior.set(this@FilterAdviceTask.duplicateClassWarningsBehavior)
      redundantPluginsBehavior.set(this@FilterAdviceTask.redundantPluginsBehavior)
      moduleStructureBehavior.set(this@FilterAdviceTask.moduleStructureBehavior)
      output.set(this@FilterAdviceTask.output)
    }
  }

  interface FilterAdviceParameters : WorkParameters {
    val projectAdvice: RegularFileProperty
    val dataBindingEnabled: Property<Boolean>
    val viewBindingEnabled: Property<Boolean>
    val anyBehavior: ListProperty<Behavior>
    val unusedDependenciesBehavior: ListProperty<Behavior>
    val usedTransitiveDependenciesBehavior: ListProperty<Behavior>
    val incorrectConfigurationBehavior: ListProperty<Behavior>
    val unusedProcsBehavior: ListProperty<Behavior>
    val compileOnlyBehavior: ListProperty<Behavior>
    val runtimeOnlyBehavior: ListProperty<Behavior>
    val duplicateClassWarningsBehavior: ListProperty<Behavior>
    val redundantPluginsBehavior: Property<Behavior>
    val moduleStructureBehavior: Property<Behavior>
    val output: RegularFileProperty
  }

  abstract class FilterAdviceAction : WorkAction<FilterAdviceParameters> {

    private val dataBindingEnabled = parameters.dataBindingEnabled.get()
    private val viewBindingEnabled = parameters.viewBindingEnabled.get()

    private val anyBehavior = partition(parameters.anyBehavior.get())
    private val unusedDependenciesBehavior = partition(parameters.unusedDependenciesBehavior.get())
    private val usedTransitiveDependenciesBehavior = partition(parameters.usedTransitiveDependenciesBehavior.get())
    private val incorrectConfigurationBehavior = partition(parameters.incorrectConfigurationBehavior.get())
    private val unusedProcsBehavior = partition(parameters.unusedProcsBehavior.get())
    private val compileOnlyBehavior = partition(parameters.compileOnlyBehavior.get())
    private val runtimeOnlyBehavior = partition(parameters.runtimeOnlyBehavior.get())
    private val duplicateClassWarningsBehavior = partition(parameters.duplicateClassWarningsBehavior.get())

    private val redundantPluginsBehavior = parameters.redundantPluginsBehavior.get()
    private val moduleStructureBehavior = parameters.moduleStructureBehavior.get()

    private fun partition(behaviors: List<Behavior>): Pair<Behavior, List<Behavior>> {
      val p = behaviors.partition { it.sourceSetName == Issue.ALL_SOURCE_SETS }

      val global = p.first.first()
      val rest = p.second

      return global to rest
    }

    override fun execute() {
      val output = parameters.output.getAndDelete()

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
        .filterNot { pa ->
          anyBehavior.first is Ignore || anyBehavior.first.filter.any { it.matches(pa.redundantPlugin) }
        }
        .filterNot { pa ->
          redundantPluginsBehavior is Ignore || redundantPluginsBehavior.filter.any { it.matches(pa.redundantPlugin) }
        }
        .toSortedSet()

      val moduleAdvice: Set<ModuleAdvice> = projectAdvice.moduleAdvice.asSequence()
        .filterNot {
          anyBehavior.first is Ignore || it.shouldIgnore(anyBehavior.first)
        }
        .filterNot {
          moduleStructureBehavior is Ignore || it.shouldIgnore(moduleStructureBehavior)
        }
        .toSortedSet()

      val duplicateClassWarnings = projectAdvice.warning.duplicateClasses.asSequence()
        .filterOf(duplicateClassWarningsBehavior)
        .toSortedSet()

      val severityHandler = SeverityHandler(
        anyBehavior = anyBehavior,
        unusedDependenciesBehavior = unusedDependenciesBehavior,
        usedTransitiveDependenciesBehavior = usedTransitiveDependenciesBehavior,
        incorrectConfigurationBehavior = incorrectConfigurationBehavior,
        unusedProcsBehavior = unusedProcsBehavior,
        compileOnlyBehavior = compileOnlyBehavior,
        duplicateClassWarningsBehavior = duplicateClassWarningsBehavior,
        redundantPluginsBehavior = redundantPluginsBehavior,
        moduleStructureBehavior = moduleStructureBehavior,
      )
      val shouldFailDeps = severityHandler.shouldFailDeps(dependencyAdvice)
      val shouldFailPlugins = severityHandler.shouldFailPlugins(pluginAdvice)
      val shouldFailModuleStructure = severityHandler.shouldFailModuleStructure(moduleAdvice)
      val shouldFailDuplicateClasses = severityHandler.shouldFailDuplicateClasses(duplicateClassWarnings)

      val filteredAdvice = projectAdvice.copy(
        dependencyAdvice = dependencyAdvice,
        pluginAdvice = pluginAdvice,
        moduleAdvice = moduleAdvice,
        warning = Warning(duplicateClassWarnings),
        shouldFail = shouldFailDeps || shouldFailPlugins || shouldFailModuleStructure || shouldFailDuplicateClasses
      )

      output.bufferWriteJson(filteredAdvice)
    }

    private fun Sequence<Advice>.filterOf(
      behaviorSpec: Pair<Behavior, List<Behavior>>,
      predicate: (Advice) -> Boolean
    ): Sequence<Advice> {
      val globalBehavior = behaviorSpec.first
      val sourceSetsBehavior = behaviorSpec.second

      val byGlobal: (Advice) -> Boolean = { a ->
        globalBehavior is Ignore
          || globalBehavior.filter.anyMatches(a.coordinates.identifier)
          || globalBehavior.filter.anyMatches(a.coordinates.gav())
      }

      val bySourceSets: (Advice) -> Boolean = { a ->
        // These are the custom behaviors, if any, associated with the source sets represented by this advice.
        val behaviors = sourceSetsBehavior.filter { b ->
          val from = a.fromConfiguration?.let { DependencyScope.sourceSetName(it) }
          val to = a.toConfiguration?.let { DependencyScope.sourceSetName(it) }

          b.sourceSetName == from || b.sourceSetName == to
        }

        // reduce() will fail on an empty collection, so use reduceOrNull().
        behaviors.map {
          it is Ignore
            || it.filter.anyMatches(a.coordinates.identifier)
            || it.filter.anyMatches(a.coordinates.gav())
        }.reduceOrNull { acc, b ->
          acc || b
        } ?: false
      }

      return filterNot { advice ->
        predicate(advice) && (byGlobal(advice) || bySourceSets(advice))
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

    private fun Sequence<DuplicateClass>.filterOf(
      behaviorSpec: Pair<Behavior, List<Behavior>>,
    ): Sequence<DuplicateClass> {
      val globalBehavior = behaviorSpec.first
      val sourceSetsBehavior = behaviorSpec.second

      val byGlobal: (DuplicateClass) -> Boolean = { d ->
        globalBehavior is Ignore
          || d.containsMatchIn(globalBehavior)
      }

      val bySourceSets: (DuplicateClass) -> Boolean = { d ->
        // These are the custom behaviors, if any, associated with the source sets represented by this warning.
        val behaviors = sourceSetsBehavior.filter { b ->
          b.sourceSetName == DependencyScope.sourceSetName(d.classpathName)
        }

        // reduce() will fail on an empty collection, so use reduceOrNull().
        behaviors.map {
          it is Ignore
            || d.containsMatchIn(it)
        }.reduceOrNull { acc, b ->
          acc || b
        } ?: false
      }

      return filterNot { duplicateClass ->
        (byGlobal(duplicateClass) || bySourceSets(duplicateClass))
      }
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
