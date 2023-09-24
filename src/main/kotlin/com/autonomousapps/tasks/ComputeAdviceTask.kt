package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.internal.Bundles
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.*
import com.autonomousapps.model.Coordinates.Companion.shallowCopy
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Configurations
import com.autonomousapps.model.declaration.Declaration
import com.autonomousapps.model.intermediates.*
import com.autonomousapps.transform.StandardTransform
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
 * Takes [usage][com.autonomousapps.model.intermediates.Usage] information from [ComputeUsagesTask] and emits the set of
 * transforms a user should perform to have correct and simple dependency declarations. I.e., produces the advice.
 */
@CacheableTask
abstract class ComputeAdviceTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Merges dependency usage reports from variant-specific computations"
  }

  @get:Input
  abstract val projectPath: Property<String>

  @get:Input
  abstract val buildPath: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyUsageReports: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyGraphViews: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val androidScoreReports: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val declarations: RegularFileProperty

  @get:Nested
  abstract val bundles: Property<DependenciesHandler.SerializableBundles>

  @get:Input
  abstract val supportedSourceSets: SetProperty<String>

  @get:Input
  abstract val ignoreKtx: Property<Boolean>

  @get:Input
  abstract val kapt: Property<Boolean>

  @get:Input
  abstract val kotlinProject: Property<Boolean>

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val redundantJvmPluginReport: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val dependencyUsages: RegularFileProperty

  @get:OutputFile
  abstract val annotationProcessorUsages: RegularFileProperty

  @get:OutputFile
  abstract val bundledTraces: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(ComputeAdviceAction::class.java) {
      projectPath.set(this@ComputeAdviceTask.projectPath)
      buildPath.set(this@ComputeAdviceTask.buildPath)
      dependencyUsageReports.set(this@ComputeAdviceTask.dependencyUsageReports)
      dependencyGraphViews.set(this@ComputeAdviceTask.dependencyGraphViews)
      androidScoreReports.set(this@ComputeAdviceTask.androidScoreReports)
      declarations.set(this@ComputeAdviceTask.declarations)
      bundles.set(this@ComputeAdviceTask.bundles)
      supportedSourceSets.set(this@ComputeAdviceTask.supportedSourceSets)
      ignoreKtx.set(this@ComputeAdviceTask.ignoreKtx)
      kapt.set(this@ComputeAdviceTask.kapt)
      kotlinProject.set(this@ComputeAdviceTask.kotlinProject)
      redundantPluginReport.set(this@ComputeAdviceTask.redundantJvmPluginReport)
      output.set(this@ComputeAdviceTask.output)
      dependencyUsages.set(this@ComputeAdviceTask.dependencyUsages)
      annotationProcessorUsages.set(this@ComputeAdviceTask.annotationProcessorUsages)
      bundledTraces.set(this@ComputeAdviceTask.bundledTraces)
    }
  }

  interface ComputeAdviceParameters : WorkParameters {
    val projectPath: Property<String>
    val buildPath: Property<String>
    val dependencyUsageReports: ListProperty<RegularFile>
    val dependencyGraphViews: ListProperty<RegularFile>
    val androidScoreReports: ListProperty<RegularFile>
    val declarations: RegularFileProperty
    val bundles: Property<DependenciesHandler.SerializableBundles>
    val supportedSourceSets: SetProperty<String>
    val ignoreKtx: Property<Boolean>
    val kapt: Property<Boolean>
    val kotlinProject: Property<Boolean>
    val redundantPluginReport: RegularFileProperty
    val output: RegularFileProperty
    val dependencyUsages: RegularFileProperty
    val annotationProcessorUsages: RegularFileProperty
    val bundledTraces: RegularFileProperty
  }

  abstract class ComputeAdviceAction : WorkAction<ComputeAdviceParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()
      val dependencyUsagesOut = parameters.dependencyUsages.getAndDelete()
      val annotationProcessorUsagesOut = parameters.annotationProcessorUsages.getAndDelete()
      val bundleTraces = parameters.bundledTraces.getAndDelete()

      val projectPath = parameters.projectPath.get()
      val buildPath = parameters.buildPath.get()
      val declarations = parameters.declarations.fromJsonSet<Declaration>()
      val dependencyGraph = parameters.dependencyGraphViews.get()
        .map { it.fromJson<DependencyGraphView>() }
        .associateBy { it.name }
      val androidScore = parameters.androidScoreReports.get()
        .map { it.fromJson<AndroidScoreVariant>() }
        .run { AndroidScore.ofVariants(this) }
        .toSetOrEmpty()
      val bundleRules = parameters.bundles.get()
      val reports = parameters.dependencyUsageReports.get().mapToSet { it.fromJson<DependencyTraceReport>() }
      val usageBuilder = UsageBuilder(
        reports = reports,
        // TODO: it would be clearer to get this from a SyntheticProject
        variants = dependencyGraph.values.map { it.variant }
      )
      val dependencyUsages = usageBuilder.dependencyUsages
      val annotationProcessorUsages = usageBuilder.annotationProcessingUsages
      val supportedSourceSets = parameters.supportedSourceSets.get()
      val isKaptApplied = parameters.kapt.get()
      val isKotlinPluginApplied = parameters.kotlinProject.get()

      val bundles = Bundles.of(
        projectPath = projectPath,
        dependencyGraph = dependencyGraph,
        bundleRules = bundleRules,
        dependencyUsages = dependencyUsages,
        ignoreKtx = parameters.ignoreKtx.get()
      )

      val dependencyAdviceBuilder = DependencyAdviceBuilder(
        projectPath = projectPath,
        buildPath = buildPath,
        bundles = bundles,
        dependencyUsages = dependencyUsages,
        annotationProcessorUsages = annotationProcessorUsages,
        declarations = declarations,
        supportedSourceSets = supportedSourceSets,
        isKaptApplied = isKaptApplied,
        isKotlinPluginApplied = isKotlinPluginApplied
      )

      val pluginAdviceBuilder = PluginAdviceBuilder(
        isKaptApplied = isKaptApplied,
        redundantPlugins = parameters.redundantPluginReport.fromNullableJsonSet<PluginAdvice>(),
        annotationProcessorUsages = annotationProcessorUsages
      )

      val projectAdvice = ProjectAdvice(
        projectPath = projectPath,
        dependencyAdvice = dependencyAdviceBuilder.advice,
        pluginAdvice = pluginAdviceBuilder.getPluginAdvice(),
        moduleAdvice = androidScore
      )

      output.bufferWriteJson(projectAdvice)
      // These must be transformed so that the Coordinates are Strings for serialization
      dependencyUsagesOut.bufferWriteJsonMap(dependencyUsages.toStringCoordinates(buildPath))
      annotationProcessorUsagesOut.bufferWriteJsonMap(annotationProcessorUsages.toStringCoordinates(buildPath))
      // TODO consider centralizing this logic in a separate PR
      bundleTraces.bufferWriteJsonSet(dependencyAdviceBuilder.bundledTraces)
    }
  }
}

internal class PluginAdviceBuilder(
  isKaptApplied: Boolean,
  redundantPlugins: Set<PluginAdvice>,
  annotationProcessorUsages: Map<Coordinates, Set<Usage>>
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
  private val supportedSourceSets: Set<String>,
  private val isKaptApplied: Boolean,
  private val isKotlinPluginApplied: Boolean,
) {

  /** The unfiltered advice. */
  val advice: Set<Advice>

  /** Dependencies that are removed from [advice] because they match a bundle rule. Used by **Reason**. */
  val bundledTraces = mutableSetOf<BundleTrace>()

  init {
    advice = computeDependencyAdvice(projectPath)
      .plus(computeAnnotationProcessorAdvice())
      .toSortedSet()
  }

  private fun computeDependencyAdvice(projectPath: String): Sequence<Advice> {
    val declarations = declarations.filterToSet { Configurations.isForRegularDependency(it.configurationName) }

    /*
     * To support KMP without providing users confusing advice, we need to do an initial pass at deps to clean up any
     * KMP deps advice.
     *
     * The primary case we are trying to catch and avoid here is one where we "fix" canonical KMP deps by trying to replace
     * them with added deps on their specific targets ("-jvm", "-android", etc). To solve this, we save off information
     * about both in the below mappings and then cross-reference them in the second pass farther down.
     *
     * Finally, in the third pass, we do our usual processing of advice but with the cleaned KMP advice. It's
     * important we clean up the KMP advice first so that the usual processing can continue to account for bundles
     * in a simple way.
     */

    // Deferred KMP advice.
    val deferredKmpAdvice = mutableListOf<Pair<Advice, Coordinates>>()
    // Deferred non-KMP advice, we'll save these for the end.
    val deferredNonKmpAdvice = mutableListOf<Pair<Advice, Coordinates>>()
    // Track a mapping of configurations to kmp canonical deps that *have targets trying to add themselves*
    // Note the values are just the canonical dep identifiers they correspond to, as the specific target isn't relevant
    val addedKmpTargets = mutableMapOf<String, MutableSet<String>>()
    // Track a mapping of removed KMP canonical deps. This is a mapping of configurations to the canonical dep identifier
    val removedKmpCanonicalDeps = mutableMapOf<String, MutableSet<String>>()

    dependencyUsages.asSequence()
      .flatMap { (coordinates, usages) ->
        StandardTransform(coordinates, declarations, supportedSourceSets, buildPath).reduce(usages).map { it to coordinates }
      }
      .forEach { (advice, originalCoordinates) ->
        when {
          advice.isAdd() && originalCoordinates is ModuleCoordinates && originalCoordinates.isKmpTargetTarget -> {
            deferredKmpAdvice += advice to originalCoordinates
            addedKmpTargets.getOrPut(advice.toConfiguration!!, ::mutableSetOf).add(originalCoordinates.kmpCommonParentIdentifier())
          }
          advice.isRemove() && originalCoordinates is ModuleCoordinates && originalCoordinates.isKmpCanonicalDependency -> {
            deferredKmpAdvice += advice to originalCoordinates
            removedKmpCanonicalDeps.getOrPut(advice.fromConfiguration!!, ::mutableSetOf).add(originalCoordinates.identifier)
          }
          else -> {
            deferredNonKmpAdvice += advice to originalCoordinates
          }
        }
      }

    val modifiedKmpAdvice = deferredKmpAdvice
      // "null" removes the advice
      .mapNotNull { (advice, originalCoordinates) ->
        when {
          // KMP target dependencies ("<dep>-android", "<dep>-jvm", etc) should defer to using their parent comment dep
          // If the parent _isn't_ present in the original declarations though, then accept it and assume they're
          // intentionally only depending on that specific target's artifact.
          advice.isAdd() && originalCoordinates.isKmpTargetThatShouldDeferToParent(advice.toConfiguration, removedKmpCanonicalDeps) -> {
            null
          }
          // This is a "misused" dep, but we still want it to use the KMP parent type rather than the targeted subtype
          advice.isAdd() && isKotlinPluginApplied && originalCoordinates is ModuleCoordinates && originalCoordinates.isKmpTargetTarget -> {
            val newIdentifier = originalCoordinates.kmpCommonParentIdentifier()
            val newCoordinates = originalCoordinates.copy(
              identifier = originalCoordinates.kmpCommonParentIdentifier(),
              gradleVariantIdentification = GradleVariantIdentification(
                capabilities = setOf(newIdentifier),
                attributes = originalCoordinates.gradleVariantIdentification.attributes,
                externalVariant = originalCoordinates.gradleVariantIdentification
              ),
            )
            advice.copy(coordinates = newCoordinates) to newCoordinates
          }
          // Don't remove KMP canonical deps, they're implicit bundles for all their underlying KMP target deps
          // Only preserve it though if a target was requested! Otherwise it's truly unused and we can nix it
          advice.isRemove() && originalCoordinates.isKmpCanonicalDependency &&
            originalCoordinates.identifier in addedKmpTargets[advice.fromConfiguration!!].orEmpty() -> {
            null
          }
          else -> advice to originalCoordinates
        }
      }

    val merged = deferredNonKmpAdvice + modifiedKmpAdvice
    return merged
      .asSequence()
      // After we remap KMP artifacts, we can sometimes remap them into otherwise-identical advice
      // To resolve this, we only process distinct advice pairs by shallow coordinates (i.e. just the
      // identifier) as Coordinates otherwise include gradle metadata in their equality test.
      .distinctBy { (advice, coordinates) ->
        advice.copy(coordinates = advice.coordinates.shallowCopy()) to coordinates.shallowCopy()
      }
      // "null" removes the advice
      .mapNotNull { (advice, originalCoordinates) ->
        // Make sure to do all operations here based on the originalCoordinates used in the graph.
        // The 'advice.coordinates' may be reduced - e.g. contain less capabilities in the GradleVariantIdentifier.
        // TODO could improve performance by merging has... with find...
        when {
          // The user cannot change this one:
          // https://github.com/gradle/gradle/blob/d9303339298e6206182fd1f5c7e51f11e4bdff30/subprojects/plugins/src/main/java/org/gradle/api/plugins/JavaTestFixturesPlugin.java#L68
          advice.coordinates.identifier == projectPath && advice.fromConfiguration?.equals("testFixturesApi") ?: false -> {
            null
          }
          // https://github.com/gradle/gradle/blob/d9303339298e6206182fd1f5c7e51f11e4bdff30/subprojects/plugins/src/main/java/org/gradle/api/plugins/JavaTestFixturesPlugin.java#L70
          advice.coordinates.identifier == projectPath && advice.fromConfiguration?.lowercase()?.endsWith("testimplementation") ?: false -> {
            null
          }
          advice.isAdd() -> {
            if (bundles.hasParentInBundle(originalCoordinates)) {
              val parent = bundles.findParentInBundle(originalCoordinates)!!
              bundledTraces += BundleTrace.DeclaredParent(parent = parent, child = originalCoordinates)
              null
            } else {
              // Optionally map given advice to "primary" advice, if bundle has a primary
              val p = bundles.maybePrimary(advice, originalCoordinates)
              if (p != advice) {
                bundledTraces += BundleTrace.PrimaryMap(primary = p.coordinates, subordinate = originalCoordinates)
              }
              p
            }
          }
          advice.isRemove() && bundles.hasUsedChild(originalCoordinates) -> {
            val child = bundles.findUsedChild(originalCoordinates)!!
            bundledTraces += BundleTrace.UsedChild(parent = originalCoordinates, child = child)
            null
          }
          // If the advice has a used child, don't change it
          advice.isAnyChange() && bundles.hasUsedChild(originalCoordinates) -> {
            val child = bundles.findUsedChild(originalCoordinates)!!
            bundledTraces += BundleTrace.UsedChild(parent = originalCoordinates, child = child)
            null
          }
          else -> advice
        }
      }
  }

  // nb: no bundle support for annotation processors
  private fun computeAnnotationProcessorAdvice(): Sequence<Advice> {
    val declarations = declarations.filterToSet { Configurations.isForAnnotationProcessor(it.configurationName) }
    return annotationProcessorUsages.asSequence()
      .flatMap { (coordinates, usages) ->
        StandardTransform(coordinates, declarations, supportedSourceSets, buildPath, isKaptApplied).reduce(usages)
      }
  }

  /**
   * Returns whether this is a KMP non-common target that can just defer to a parent common declaration in
   * [kmpCommonDeclarations].
   *
   * @see GradleVariantIdentification.kmpAttribute
   */
  private fun Coordinates.isKmpTargetThatShouldDeferToParent(
    targetConfiguration: String?,
    kmpCommonDeclarations: Map<String, Set<String>>
  ): Boolean {
    // This only applies to module coordinates.
    if (this !is ModuleCoordinates) return false
    if (targetConfiguration == null) return false
    if (isKmpTargetTarget) {
      val commonDeclarationsInConfiguration = kmpCommonDeclarations[targetConfiguration] ?: return false
      val expectedParent = kmpCommonParentIdentifier()
      return expectedParent in commonDeclarationsInConfiguration
    }
    return false
  }
}

/**
 * Equivalent to
 * ```
 * someBoolean.also { b ->
 *   if (b) block()
 * }
 * ```
 */
internal inline fun Boolean.andIfTrue(block: () -> Unit): Boolean {
  if (this) {
    block()
  }
  return this
}
