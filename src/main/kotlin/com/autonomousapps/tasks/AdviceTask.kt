@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.Behavior
import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.*
import com.autonomousapps.internal.advice.*
import com.autonomousapps.internal.utils.chatter
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Produces human- and machine-readable advice on how to modify a project's dependencies in order to have a healthy
 * build.
 */
@CacheableTask
abstract class AdviceTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Provides advice on how best to declare the project's dependencies"
  }

  /**
   * A [`Set<Component>`][Component].
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val allComponentsReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val unusedDependenciesReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val usedTransitiveDependenciesReport: RegularFileProperty

  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val abiDependenciesReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val allDeclaredDependenciesReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val unusedProcsReport: RegularFileProperty

  // TODO all the "configuration" inputs below should be coalesced into a single object for simplicity

  @get:Input
  abstract val ignoreKtx: Property<Boolean>

  @get:Input
  abstract val dataBindingEnabled: Property<Boolean>

  @get:Input
  abstract val viewBindingEnabled: Property<Boolean>

  @get:Input
  abstract val failOnAny: Property<Behavior>

  @get:Input
  abstract val failOnUnusedDependencies: Property<Behavior>

  @get:Input
  abstract val failOnUsedTransitiveDependencies: Property<Behavior>

  @get:Input
  abstract val failOnIncorrectConfiguration: Property<Behavior>

  @get:Input
  abstract val chatty: Property<Boolean>

  @get:OutputFile
  abstract val adviceReport: RegularFileProperty

  private val chatter by lazy { chatter(chatty.get()) }

  @TaskAction
  fun action() {
    // Output
    val adviceFile = adviceReport.get().asFile
    adviceFile.delete()

    // Inputs
    val allComponents = allComponentsReport.get().asFile.readText().fromJsonList<Component>()
    val unusedDirectComponents =
      unusedDependenciesReport.get().asFile.readText().fromJsonList<UnusedDirectComponent>()
    val usedTransitiveComponents =
      usedTransitiveDependenciesReport.get().asFile.readText().fromJsonList<TransitiveComponent>()
    val abiDeps = abiDependenciesReport.orNull?.asFile?.readText()?.fromJsonList<Dependency>()
      ?: emptyList()
    val allDeclaredDeps =
      allDeclaredDependenciesReport.get().asFile.readText().fromJsonList<Artifact>()
        .map { it.dependency }
        .filter { it.configurationName != null }
    val unusedProcs = unusedProcsReport.get().asFile.readText().fromJsonSet<AnnotationProcessor>()

    // Print to the console several lists:
    // 1. Dependencies that should be removed.
    // 2. Dependencies that are already declared and whose configurations should be modified.
    // 3. Dependencies that should be added and the configurations on which to add them.
    // 4. Dependencies that are candidates to be compileOnly, but aren't currently.
    // 5. Annotation processors that are declared but are not used.

    val advisor = Advisor(
      allComponents = allComponents,
      unusedDirectComponents = unusedDirectComponents,
      usedTransitiveComponents = usedTransitiveComponents,
      abiDeps = abiDeps,
      allDeclaredDeps = allDeclaredDeps,
      unusedProcs = unusedProcs,
      ignoreKtx = ignoreKtx.get()
    )

    val computedAdvice = advisor.compute(filterSpecBuilder())
    val advices = computedAdvice.getAdvices()
    val advicePrinter = AdvicePrinter(computedAdvice)

    var didGiveAdvice = false

    if (!computedAdvice.filterRemove) {
      advicePrinter.getRemoveAdvice()?.let {
        chatter.chat("Unused dependencies which should be removed:\n$it\n")
        didGiveAdvice = true
      }
    }

    if (!computedAdvice.filterAdd) {
      advicePrinter.getAddAdvice()?.let {
        chatter.chat("Transitively used dependencies that should be declared directly as indicated:\n$it\n")
        didGiveAdvice = true
      }
    }

    if (!computedAdvice.filterChange) {
      advicePrinter.getChangeAdvice()?.let {
        chatter.chat("Existing dependencies which should be modified to be as indicated:\n$it\n")
        didGiveAdvice = true
      }
    }

    if (!computedAdvice.filterCompileOnly) {
      advicePrinter.getCompileOnlyAdvice()?.let {
        chatter.chat("EXPERIMENTAL. See README for heuristic used\nDependencies which could be compile-only:\n$it\n")
        didGiveAdvice = true
      }
    }

    // TODO add filter
    advicePrinter.getRemoveProcAdvice()?.let {
      chatter.chat("Unused annotation processors that should be removed:\n$it\n")
      didGiveAdvice = true
    }

    if (didGiveAdvice) {
      chatter.chat("See machine-readable report at ${adviceFile.path}")
    } else {
      chatter.chat("Looking good! No changes needed")
    }

    adviceFile.writeText(advices.toJson())
  }

  private fun filterSpecBuilder() = FilterSpecBuilder().apply {
    universalFilter = CompositeFilter(filters)
    anyBehavior = failOnAny.get()
    unusedDependenciesBehavior = failOnUnusedDependencies.get()
    usedTransitivesBehavior = failOnUsedTransitiveDependencies.get()
    incorrectConfigurationsBehavior = failOnIncorrectConfiguration.get()
  }

  private val filters: List<DependencyFilter> by lazy(mode = LazyThreadSafetyMode.NONE) {
    val filters = mutableListOf<DependencyFilter>()
    if (dataBindingEnabled.get()) {
      filters.add(DataBindingFilter())
    }
    if (viewBindingEnabled.get()) {
      filters.add(ViewBindingFilter())
    }
    filters
  }
}
