@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.internal.*
import com.autonomousapps.internal.advice.Advisor
import com.autonomousapps.internal.advice.filter.*
import com.autonomousapps.internal.utils.*
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Produces human- and machine-readable advice on how to modify a project's dependencies in order to
 * have a healthy build.
 */
@CacheableTask
abstract class AdvicePerVariantTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
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

  @get:Internal
  lateinit var dependenciesHandler: DependenciesHandler

  @Input
  fun getDependencyBundles(): Map<String, Set<Regex>> {
    return dependenciesHandler.bundles.asMap.map { (name, groups) ->
      name to groups.includes.get()
    }.toMap()
  }

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

  @get:Internal
  abstract val inMemoryCacheProvider: Property<InMemoryCache>

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
    val abiDeps = abiDependenciesReport.orNull?.asFile?.readText()?.fromJsonSet<PublicComponent>()
      ?.mapToSet { it.dependency }
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
      dependencyBundles = getDependencyBundles(),
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
      logger.debug("See machine-readable report at ${adviceFile.path}")
      logger.debug("See pretty report at ${advicePrettyFile.path}")
      logger.debug("See machine-readable console report at ${adviceConsoleFile.path}")
      logger.debug("See pretty console report at ${adviceConsolePrettyFile.path}")
    }
  }

  private fun filterSpecBuilder() = FilterSpecBuilder().apply {
    universalFilter = CompositeFilter(filters)
    anyBehavior = this@AdvicePerVariantTask.anyBehavior.get()
    unusedDependenciesBehavior = this@AdvicePerVariantTask.unusedDependenciesBehavior.get()
    usedTransitivesBehavior = usedTransitiveDependenciesBehavior.get()
    incorrectConfigurationsBehavior = incorrectConfigurationBehavior.get()
    unusedProcsBehavior = this@AdvicePerVariantTask.unusedProcsBehavior.get()
    compileOnlyBehavior = this@AdvicePerVariantTask.compileOnlyBehavior.get()
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
