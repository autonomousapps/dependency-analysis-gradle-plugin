@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.extension.Behavior
import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.*
import com.autonomousapps.internal.advice.Advisor
import com.autonomousapps.internal.advice.filter.*
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
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
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val allComponentsReport: RegularFileProperty

  /**
   * A [`Set<ComponentWithTransitives>`][ComponentWithTransitives].
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val allComponentsWithTransitives: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val unusedDependenciesReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val usedTransitiveDependenciesReport: RegularFileProperty

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val abiDependenciesReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val allDeclaredDependenciesReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val unusedProcsReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val serviceLoaders: RegularFileProperty

  /**
   * [`Set<VariantDependency>`][VariantDependency]
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val usedVariantDependencies: RegularFileProperty

  @get:Input
  abstract val facadeGroups: SetProperty<String>

  @get:Input
  abstract val ignoreKtx: Property<Boolean>

  @get:Input
  abstract val dataBindingEnabled: Property<Boolean>

  @get:Input
  abstract val viewBindingEnabled: Property<Boolean>

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
  abstract val failOnUnusedProcs: Property<Behavior>

  @get:Input
  abstract val failOnCompileOnly: Property<Behavior>

  /**
   * Log level.
   */
  @get:Input
  abstract val chatty: Property<Boolean>

  /*
   * Outputs
   */

  @get:OutputFile
  abstract val adviceReport: RegularFileProperty

  @get:OutputFile
  abstract val advicePrettyReport: RegularFileProperty

  @get:OutputFile
  abstract val adviceConsoleReport: RegularFileProperty

  @get:OutputFile
  abstract val adviceConsolePrettyReport: RegularFileProperty

  private val chatter by lazy { chatter(chatty.get()) }

  @TaskAction
  fun action() {
    // Output
    val adviceFile = adviceReport.getAndDelete()
    val advicePrettyFile = advicePrettyReport.getAndDelete()
    val adviceConsoleFile = adviceConsoleReport.getAndDelete()
    val adviceConsolePrettyFile = adviceConsolePrettyReport.getAndDelete()

    // Inputs
    val usedVariantDependencies = usedVariantDependencies.fromJsonSet<VariantDependency>()
    val allComponents = allComponentsReport.fromJsonSet<Component>()
    val allComponentsWithTransitives = allComponentsWithTransitives.fromJsonSet<ComponentWithTransitives>()
    val unusedDirectComponents = unusedDependenciesReport.fromJsonSet<ComponentWithTransitives>()
    val usedTransitiveComponents = usedTransitiveDependenciesReport.fromJsonSet<TransitiveComponent>()
    val abiDeps = abiDependenciesReport.orNull?.asFile?.readText()?.fromJsonSet<Dependency>()
      ?: emptySet()
    val allDeclaredDeps = allDeclaredDependenciesReport.fromJsonSet<Artifact>()
      .mapToSet { it.dependency }
      .filterToSet { it.configurationName != null }
    val unusedProcs = unusedProcsReport.fromJsonSet<AnnotationProcessor>()

    // Print to the console several lists:
    // 1. Dependencies that should be removed.
    // 2. Dependencies that are already declared and whose configurations should be modified.
    // 3. Dependencies that should be added and the configurations on which to add them.
    // 4. Dependencies that are candidates to be compileOnly, but aren't currently.
    // 5. Annotation processors that are declared but are not used.

    val advisor = Advisor(
      usedVariantDependencies = usedVariantDependencies,
      allComponents = allComponents,
      allComponentsWithTransitives = allComponentsWithTransitives,
      unusedComponentsWithTransitives = unusedDirectComponents,
      usedTransitiveComponents = usedTransitiveComponents,
      abiDeps = abiDeps,
      allDeclaredDeps = allDeclaredDeps,
      unusedProcs = unusedProcs,
      serviceLoaders = serviceLoaders.fromJsonSet(),
      facadeGroups = facadeGroups.get(),
      ignoreKtx = ignoreKtx.get()
    )

    val computedAdvice = advisor.compute(filterSpecBuilder())
    val advices = computedAdvice.getAdvices()
    val consoleReport = ConsoleReport.from(computedAdvice)

    adviceFile.writeText(advices.toJson())
    advicePrettyFile.writeText(advices.toPrettyString())
    adviceConsoleFile.writeText(consoleReport.toJson())
    adviceConsolePrettyFile.writeText(consoleReport.toPrettyString())

    if (advices.isNotEmpty()) {
      chatter.chat("See machine-readable report at ${adviceFile.path}")
      chatter.chat("See pretty report at ${advicePrettyFile.path}")
      chatter.chat("See machine-readable console report at ${adviceConsoleFile.path}")
      chatter.chat("See pretty console report at ${adviceConsolePrettyFile.path}")
    }
  }

  private fun filterSpecBuilder() = FilterSpecBuilder().apply {
    universalFilter = CompositeFilter(filters)
    anyBehavior = failOnAny.get()
    unusedDependenciesBehavior = failOnUnusedDependencies.get()
    usedTransitivesBehavior = failOnUsedTransitiveDependencies.get()
    incorrectConfigurationsBehavior = failOnIncorrectConfiguration.get()
    unusedProcsBehavior = failOnUnusedProcs.get()
    compileOnlyBehavior = failOnCompileOnly.get()
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
