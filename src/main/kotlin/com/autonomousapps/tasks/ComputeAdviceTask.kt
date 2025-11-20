// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.ProjectType
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.internal.Bundles
import com.autonomousapps.internal.UsageContainer
import com.autonomousapps.internal.transform.StandardTransform
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.*
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Takes [usage][com.autonomousapps.model.internal.intermediates.Usage] information from [ComputeUsagesTask] and emits
 * the set of transforms a user should perform to have correct and simple dependency declarations. I.e., produces the
 * advice.
 */
@CacheableTask
public abstract class ComputeAdviceTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Merges dependency usage reports from variant-specific computations"
  }

  @get:Input
  public abstract val projectPath: Property<String>

  @get:Input
  public abstract val buildPath: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val dependencyUsageReports: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val dependencyGraphViews: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val androidScoreReports: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val declarations: RegularFileProperty

  @get:Nested
  public abstract val bundles: Property<DependenciesHandler.SerializableBundles>

  @get:Input
  public abstract val supportedSourceSets: SetProperty<String>

  @get:Input
  public abstract val ignoreKtx: Property<Boolean>

  @get:Input
  public abstract val explicitSourceSets: SetProperty<String>

  @get:Input
  public abstract val projectType: Property<ProjectType>

  @get:Input
  public abstract val kapt: Property<Boolean>

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val redundantJvmPluginReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val duplicateClassesReports: ListProperty<RegularFile>

  /*
   * Outputs.
   */

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @get:OutputFile
  public abstract val dependencyUsages: RegularFileProperty

  @get:OutputFile
  public abstract val annotationProcessorUsages: RegularFileProperty

  @get:OutputFile
  public abstract val bundledTraces: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(ComputeAdviceAction::class.java) {
      it.projectPath.set(projectPath)
      it.buildPath.set(buildPath)
      it.dependencyUsageReports.set(dependencyUsageReports)
      it.dependencyGraphViews.set(dependencyGraphViews)
      it.androidScoreReports.set(androidScoreReports)
      it.declarations.set(declarations)
      it.bundles.set(bundles)
      it.supportedSourceSets.set(supportedSourceSets)
      it.ignoreKtx.set(ignoreKtx)
      it.explicitSourceSets.set(explicitSourceSets)
      it.projectType.set(projectType)
      it.kapt.set(kapt)
      it.redundantPluginReport.set(redundantJvmPluginReport)
      it.duplicateClassesReports.set(duplicateClassesReports)

      it.output.set(output)
      it.dependencyUsages.set(dependencyUsages)
      it.annotationProcessorUsages.set(annotationProcessorUsages)
      it.bundledTraces.set(bundledTraces)
    }
  }

  public interface ComputeAdviceParameters : WorkParameters {
    public val projectPath: Property<String>
    public val buildPath: Property<String>
    public val dependencyUsageReports: ListProperty<RegularFile>
    public val dependencyGraphViews: ListProperty<RegularFile>
    public val androidScoreReports: ListProperty<RegularFile>
    public val declarations: RegularFileProperty
    public val bundles: Property<DependenciesHandler.SerializableBundles>
    public val supportedSourceSets: SetProperty<String>
    public val ignoreKtx: Property<Boolean>
    public val explicitSourceSets: SetProperty<String>
    public val projectType: Property<ProjectType>
    public val kapt: Property<Boolean>
    public val redundantPluginReport: RegularFileProperty
    public val duplicateClassesReports: ListProperty<RegularFile>

    public val output: RegularFileProperty
    public val dependencyUsages: RegularFileProperty
    public val annotationProcessorUsages: RegularFileProperty
    public val bundledTraces: RegularFileProperty
  }

  public abstract class ComputeAdviceAction : WorkAction<ComputeAdviceParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()
      val dependencyUsagesOut = parameters.dependencyUsages.getAndDelete()
      val annotationProcessorUsagesOut = parameters.annotationProcessorUsages.getAndDelete()
      val bundleTraces = parameters.bundledTraces.getAndDelete()

      val projectPath = parameters.projectPath.get()
      val buildPath = parameters.buildPath.get()
      val declarations = parameters.declarations.fromJsonSet<Declaration>().toSortedSet()
      val dependencyGraph = DependencyGraphView.asMap(parameters.dependencyGraphViews)
      val androidScore = parameters.androidScoreReports.get()
        .map { it.fromJson<AndroidScoreVariant>() }
        .run { AndroidScore.ofVariants(this) }
        .toSetOrEmpty()
      val bundleRules = parameters.bundles.get()
      val traces = parameters.dependencyUsageReports.get().mapToSet { it.fromJson<DependencyTraceReport>() }
      val usageBuilder = UsageBuilder(
        traces = traces,
        // TODO(tsr): it would be clearer to get this from a SyntheticProject
        // TODO(tsr): this now includes the runtime graph. Maybe strip it if it's problematic here
        sourceKinds = dependencyGraph.values.map { it.sourceKind },
      )
      val dependencyUsages = usageBuilder.dependencyUsages
      val annotationProcessorUsages = usageBuilder.annotationProcessingUsages
      val supportedSourceSets = parameters.supportedSourceSets.get()
      val explicitSourceSets = parameters.explicitSourceSets.get()
      val projectType = parameters.projectType.get()
      val isKaptApplied = parameters.kapt.get()
      val ignoreKtx = parameters.ignoreKtx.get()
      val configurationNames = ConfigurationNames(projectType, supportedSourceSets)

      val bundles = Bundles.of(
        projectPath = projectPath,
        dependencyGraph = dependencyGraph,
        bundleRules = bundleRules,
        dependencyUsages = dependencyUsages,
        declarations = declarations,
        configurationNames = configurationNames,
        ignoreKtx = ignoreKtx,
      )

      val dependencyAdviceBuilder = DependencyAdviceBuilder(
        projectPath = projectPath,
        buildPath = buildPath,
        bundles = bundles,
        dependencyUsages = dependencyUsages,
        annotationProcessorUsages = annotationProcessorUsages,
        declarations = declarations,
        dependencyGraph = dependencyGraph,
        supportedSourceSets = supportedSourceSets,
        explicitSourceSets = explicitSourceSets,
        projectType = projectType,
        configurationNames = configurationNames,
        isKaptApplied = isKaptApplied,
      )

      val pluginAdviceBuilder = PluginAdviceBuilder(
        isKaptApplied = isKaptApplied,
        redundantPlugins = parameters.redundantPluginReport.fromNullableJsonSet<PluginAdvice>(),
        annotationProcessorUsages = annotationProcessorUsages,
      )

      val projectAdvice = ProjectAdvice(
        projectPath = projectPath,
        dependencyAdvice = dependencyAdviceBuilder.advice,
        pluginAdvice = pluginAdviceBuilder.getPluginAdvice(),
        moduleAdvice = androidScore,
        warning = buildWarning(),
      )

      output.bufferWriteJson(projectAdvice)
      dependencyUsagesOut.bufferWriteJson(UsageContainer.of(dependencyUsages))
      annotationProcessorUsagesOut.bufferWriteJson(UsageContainer.of(annotationProcessorUsages))
      bundleTraces.bufferWriteJsonSet(dependencyAdviceBuilder.bundledTraces)
    }

    private fun buildWarning(): Warning {
      val duplicateClassesReports = parameters.duplicateClassesReports.get().asSequence()
        .map { it.fromJsonSet<DuplicateClass>() }
        .flatten()
        .toSortedSet()

      return Warning(duplicateClassesReports)
    }
  }
}

internal class PluginAdviceBuilder(
  isKaptApplied: Boolean,
  redundantPlugins: Set<PluginAdvice>,
  annotationProcessorUsages: Map<Coordinates, Set<Usage>>,
) {

  private val pluginAdvice = mutableSetOf<PluginAdvice>()

  fun getPluginAdvice(): Set<PluginAdvice> = pluginAdvice

  init {
    pluginAdvice.addAll(redundantPlugins)

    if (isKaptApplied) {
      val usedProcs = annotationProcessorUsages.asSequence()
        .filter { (_, usages) -> usages.any { it.bucket == Bucket.ANNOTATION_PROCESSOR } }
        .map { it.key }
        .toSet()

      // kapt is unused
      if (usedProcs.isEmpty()) {
        pluginAdvice.add(PluginAdvice.redundantKapt())
      }
    }
  }
}

internal class DependencyAdviceBuilder(
  projectPath: String,
  private val buildPath: String,
  private val bundles: Bundles,
  private val dependencyUsages: Map<Coordinates, Set<Usage>>,
  private val annotationProcessorUsages: Map<Coordinates, Set<Usage>>,
  private val declarations: Set<Declaration>,
  private val dependencyGraph: Map<String, DependencyGraphView>,
  private val supportedSourceSets: Set<String>,
  private val explicitSourceSets: Set<String>,
  private val projectType: ProjectType,
  private val configurationNames: ConfigurationNames,
  private val isKaptApplied: Boolean,
) {

  /** The unfiltered advice. */
  val advice: Set<Advice>

  /** Dependencies that are removed from [advice] because they match a bundle rule. Used by **Reason**. */
  val bundledTraces: MutableSet<BundleTrace> = sortedSetOf()

  init {
    advice = computeDependencyAdvice(projectPath)
      .plus(computeAnnotationProcessorAdvice())
      .toSortedSet()
  }

  private fun computeDependencyAdvice(projectPath: String): Sequence<Advice> {
    val declarations =
      declarations.filterToOrderedSet { configurationNames.isForRegularDependency(it.configurationName) }

    fun Advice.isRemoveTestDependencyOnSelf(): Boolean {
      return coordinates.identifier == projectPath
        // https://github.com/gradle/gradle/blob/d9303339298e6206182fd1f5c7e51f11e4bdff30/subprojects/plugins/src/main/java/org/gradle/api/plugins/JavaTestFixturesPlugin.java#L68
        && (fromConfiguration?.equals("testFixturesApi") == true
        // https://github.com/gradle/gradle/blob/d9303339298e6206182fd1f5c7e51f11e4bdff30/subprojects/plugins/src/main/java/org/gradle/api/plugins/JavaTestFixturesPlugin.java#L70
        || fromConfiguration?.lowercase()?.endsWith("testimplementation") == true)
    }

    fun Advice.isAddTestDependencyOnSelf(): Boolean {
      return coordinates.identifier == projectPath
        && (fromConfiguration == null && toConfiguration?.equals("testImplementation") == true)
    }

    return dependencyUsages.asSequence()
      .flatMap { (coordinates, usages) ->
        StandardTransform(
          coordinates = coordinates,
          declarations = declarations,
          dependencyGraph = dependencyGraph,
          configurationNames = configurationNames,
          buildPath = buildPath,
          explicitSourceSets = explicitSourceSets,
          projectType = projectType,
        )
          .reduce(usages)
          .map { advice -> advice to coordinates }
      }
      // "null" removes the advice
      .mapNotNull { (advice, originalCoordinates) ->
        // Make sure to do all operations here based on the originalCoordinates used in the graph.
        // The 'advice.coordinates' may be reduced - e.g. contain fewer capabilities in the GradleVariantIdentifier.
        when {
          // The user cannot change these
          advice.isRemoveTestDependencyOnSelf() -> null

          // The user should not have to add a test dependency on self
          advice.isAddTestDependencyOnSelf() -> null

          // TODO(tsr): Update bundledTraces and Reason? The current output is OK, but lacks specificity in the non-null
          //  `parentAdvice` case.
          // This can transform add-advice to change-advice. Currently only for KMP projects where the "parent" KMP dep
          // is declared on a commonX configuration, and the "child" -jvm or -android dep needs to be upgraded.
          advice.isAdd() && bundles.hasParentInBundle(originalCoordinates) -> {
            val parent = bundles.findParentInBundle(originalCoordinates)!!
            bundledTraces.add(BundleTrace.DeclaredParent(parent = parent, child = originalCoordinates))

            val parentAdvice = bundles.maybeParent(advice, originalCoordinates)
            if (parentAdvice != advice) {
              parentAdvice
            } else {
              null
            }
          }

          // Optionally map given advice to "primary" advice, if bundle has a primary
          advice.isAdd() -> {
            val p = bundles.maybePrimary(advice, originalCoordinates)
            if (p != advice) {
              bundledTraces.add(BundleTrace.PrimaryMap(primary = p.coordinates, subordinate = originalCoordinates))
            }
            p
          }

          advice.isRemove() && bundles.hasUsedChild(originalCoordinates) -> {
            val child = bundles.findUsedChild(originalCoordinates)!!
            bundledTraces.add(BundleTrace.UsedChild(parent = originalCoordinates, child = child))
            null
          }

          // If the advice has a used child, don't change it
          advice.isAnyChange() && bundles.hasUsedChild(originalCoordinates) -> {
            val child = bundles.findUsedChild(originalCoordinates)!!
            bundledTraces.add(BundleTrace.UsedChild(parent = originalCoordinates, child = child))
            null
          }

          // TODO(tsr): extract this into something unit-testable.
          // For KMP projects, if the advice is to move the dependency from a commonX source set to a specific target,
          // ignore it for now. If the advice is to move and upgrade, then just upgrade but keep in the same source set.
          projectType == ProjectType.KMP && advice.isAnyChange() -> {
            val fromConfiguration = advice.fromConfiguration!!
            val toConfiguration = advice.toConfiguration!!
            val fromCommon = fromConfiguration.startsWith("commonTest") || fromConfiguration.startsWith("commonMain")
            val toCommon = toConfiguration.startsWith("commonTest") || fromConfiguration.startsWith("commonMain")

            if (fromCommon && !toCommon) {
              if (fromConfiguration.endsWith("Implementation") && toConfiguration.endsWith("Api")) {
                val newConfiguration = fromConfiguration.substringBeforeLast("Implementation") + "Api"
                advice.copy(toConfiguration = newConfiguration)
              } else {
                null
              }
            } else {
              advice
            }
          }

          else -> advice
        }
      }
  }

  // nb: no bundle support for annotation processors
  private fun computeAnnotationProcessorAdvice(): Sequence<Advice> {
    val declarations = declarations
      .filterToOrderedSet { configurationNames.isForAnnotationProcessor(it.configurationName) }
    return annotationProcessorUsages.asSequence()
      .flatMap { (coordinates, usages) ->
        StandardTransform(
          coordinates = coordinates,
          declarations = declarations,
          dependencyGraph = emptyMap(),
          buildPath = buildPath,
          explicitSourceSets = explicitSourceSets,
          projectType = projectType,
          configurationNames = configurationNames,
          isKaptApplied = isKaptApplied,
        ).reduce(usages)
      }
  }
}
