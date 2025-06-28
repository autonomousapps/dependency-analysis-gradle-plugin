// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.internal.reason.DependencyAdviceExplainer
import com.autonomousapps.internal.reason.ModuleAdviceExplainer
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.CoordinatesString.Companion.equalsKey
import com.autonomousapps.internal.utils.CoordinatesString.Companion.firstCoordinatesKeySegment
import com.autonomousapps.internal.utils.CoordinatesString.Companion.matchesKey
import com.autonomousapps.internal.utils.CoordinatesString.Companion.secondCoordinatesKeySegment
import com.autonomousapps.internal.utils.strings.replaceExceptLast
import com.autonomousapps.model.*
import com.autonomousapps.model.Coordinates.Companion.copy
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.intermediates.BundleTrace
import com.autonomousapps.model.internal.intermediates.Usage
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class ReasonTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Explain how a dependency is used"
  }

  @get:Input
  abstract val rootProjectName: Property<String>

  @get:Input
  abstract val projectPath: Property<String>

  /**
   * The dependency identifier or GAV coordinates being queried. By default, reports on the main [capability].
   *
   * See also [capability] and [module].
   */
  @get:Optional
  @get:Input
  @set:Option(
    option = "id",
    description = "The dependency you'd like to reason about (com.foo:bar:1.0 or :other:module)"
  )
  var id: String? = null

  /**
   * The capability to be queried. If not specified, defaults to main capability.
   *
   * See also [id].
   */
  @get:Optional
  @get:Input
  @set:Option(
    option = "capability",
    description = "The capability you're interested in. Defaults to main capability. A typical option is 'test-fixtures'"
  )
  var capability: String? = null

  /**
   * The category of module-structure advice to query for. Only available option at this time is 'android'.
   *
   * See also [id].
   */
  @get:Optional
  @get:Input
  @set:Option(
    option = "module",
    description = "The module-structure-related advice you'd like more insight into ('android')"
  )
  var module: String? = null

  @get:Input
  abstract val dependencyMap: MapProperty<String, String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val dependencyUsageReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val annotationProcessorUsageReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val unfilteredAdviceReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val finalAdviceReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val bundleTracesReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyGraphViews: ListProperty<RegularFile>

  // TODO InputDirectory of all dependencies for finding capabilities

  @TaskAction fun action() {
    val options = options()

    // Explain dependency advice
    options.id?.let { dependency ->
      workerExecutor.noIsolation().submit(ExplainDependencyAdviceAction::class.java) {
        id.set(dependency)
        capability.set(options.capability ?: "")
        rootProjectName.set(this@ReasonTask.rootProjectName)
        projectPath.set(this@ReasonTask.projectPath)
        dependencyMap.set(this@ReasonTask.dependencyMap)
        dependencyUsageReport.set(this@ReasonTask.dependencyUsageReport)
        annotationProcessorUsageReport.set(this@ReasonTask.annotationProcessorUsageReport)
        unfilteredAdviceReport.set(this@ReasonTask.unfilteredAdviceReport)
        finalAdviceReport.set(this@ReasonTask.finalAdviceReport)
        bundleTracesReport.set(this@ReasonTask.bundleTracesReport)
        dependencyGraphViews.set(this@ReasonTask.dependencyGraphViews)
      }
    }

    // Explain module structure advice
    options.module?.let { moduleStructure ->
      workerExecutor.noIsolation().submit(ExplainModuleAdviceAction::class.java) {
        module.set(moduleStructure)
        projectPath.set(this@ReasonTask.projectPath)
        unfilteredAdviceReport.set(this@ReasonTask.unfilteredAdviceReport)
        finalAdviceReport.set(this@ReasonTask.finalAdviceReport)
      }
    }
  }

  private fun options(): Options {
    val id = id
    val module = module
    val capability = capability

    // One of these must be non-null, or there is no valid request.
    if (id == null && module == null) {
      throw InvalidUserDataException(help())
    }

    // capability only makes sense if the user also is making an id request.
    if (capability != null && id == null) {
      throw InvalidUserDataException(help())
    }

    return Options(id = id, capability = capability, module = module)
  }

  private class Options(
    val id: String?,
    val capability: String?,
    val module: String?,
  )

  private fun help() = projectPath.get().let { path ->
    """
      You must call 'reason' with either the `--id` or `--module` option, or both.
      You may also specify a `--capability`, but this only influences the results of an `--id` query.
      
      Usage for --id:
        ./gradlew ${path}:reason --id com.foo:bar:1.0
        ./gradlew ${path}:reason --id com.foo:bar
        ./gradlew ${path}:reason --id :other:module
        ./gradlew ${path}:reason --id <dependency identifier> --capability test-fixtures
        
      For external dependencies, the version is optional.
      Capability is optional. If unspecified, defaults to main capability.
      
      Usage for --module:
        ./gradlew ${path}:reason --module android
    """.trimIndent()
  }

  interface ExplainDependencyAdviceParams : WorkParameters {
    val id: Property<String>
    val capability: Property<String>
    val rootProjectName: Property<String>
    val projectPath: Property<String>
    val dependencyMap: MapProperty<String, String>
    val dependencyUsageReport: RegularFileProperty
    val annotationProcessorUsageReport: RegularFileProperty
    val unfilteredAdviceReport: RegularFileProperty
    val finalAdviceReport: RegularFileProperty
    val bundleTracesReport: RegularFileProperty
    val dependencyGraphViews: ListProperty<RegularFile>
  }

  abstract class ExplainDependencyAdviceAction : WorkAction<ExplainDependencyAdviceParams> {

    private val logger = getLogger<ReasonTask>()

    private val capability = parameters.capability.get()
    private val rootProjectName = parameters.rootProjectName.get()
    private val projectPath = parameters.projectPath.get()
    private val dependencyGraph = parameters.dependencyGraphViews.get()
      .map { it.fromJson<DependencyGraphView>() }
      .associateBy { "${it.name},${it.configurationName}" }
    private val unfilteredProjectAdvice = parameters.unfilteredAdviceReport.fromJson<ProjectAdvice>()
    private val finalProjectAdvice = parameters.finalAdviceReport.fromJson<ProjectAdvice>()
    private val dependencyMap = parameters.dependencyMap.get().toLambda()

    private val dependencyUsages = parameters.dependencyUsageReport.fromJsonMapSet<String, Usage>()
    private val annotationProcessorUsages = parameters.annotationProcessorUsageReport.fromJsonMapSet<String, Usage>()

    // Derived from the above
    private val targetCoord = getRequestedCoordinates(true)
    private val requestedCoord = getRequestedCoordinates(false)
    private val finalAdvice = findAdviceIn(finalProjectAdvice)
    private val unfilteredAdvice = findAdviceIn(unfilteredProjectAdvice)
    private val usages = getUsageFor(targetCoord)

    override fun execute() {
      val reason = DependencyAdviceExplainer(
        project = ProjectCoordinates(projectPath, GradleVariantIdentification(setOf("ROOT"), emptyMap()), ":"),
        requested = requestedCoord,
        target = targetCoord,
        requestedCapability = capability,
        usages = usages,
        advice = finalAdvice,
        dependencyGraph = dependencyGraph,
        bundleTraces = bundleTraces(),
        wasFiltered = wasFiltered(),
        dependencyMap = dependencyMap,
      ).computeReason()

      logger.quiet(reason)
    }

    /**
     * Returns the requested ID as [Coordinates], even if user passed in a prefix.
     *
     * `normalized == true` to return 'group:coordinate' notation even if the user requested :project-path notation.
     */
    private fun getRequestedCoordinates(normalize: Boolean): Coordinates {
      val requestedId = parameters.id.get()
      val requestedCapability = capability
      val requestedViaProjectPath = requestedId.startsWith(":")

      fun findInGraph(): String? = dependencyGraph.values.asSequence()
        .flatMap { it.nodes }
        .find { coordinates ->
          val gav = coordinates.gav()
          gav == requestedId
            || gav.startsWith("$requestedId:")
            || dependencyMap(gav) == requestedId
            || dependencyMap(coordinates.identifier) == requestedId
        }?.gav()

      // Guaranteed to find full GAV or throw
      val gavKey = findFilteredDependencyKey(dependencyUsages.entries, requestedId)
        ?: findFilteredDependencyKey(annotationProcessorUsages.entries, requestedId)
        ?: findInGraph()
        ?: throw InvalidUserDataException("There is no dependency with coordinates '$requestedId' in this project.")

      val gav = if (requestedViaProjectPath && !normalize) {
        secondCoordinatesKeySegment(gavKey) ?: gavKey
      } else {
        firstCoordinatesKeySegment(gavKey)
      }

      val capabilitySuffix = if (requestedCapability.isEmpty()) {
        ""
      } else if (requestedCapability == "testFixtures") {
        GradleVariantIdentification.TEST_FIXTURES
      } else {
        "-$requestedCapability"
      }

      val baseCapability = CoordinatesString.of(gavKey).capabilities?.singleOrNull() ?: ""
      val syntheticCapability = "$baseCapability$capabilitySuffix"

      val includedBuildId = if (requestedId.count { it == ':' } == 1) {
        "$rootProjectName$requestedId"
      } else {
        "$rootProjectName${requestedId.replaceExceptLast(":", ".")}"
      }

      // In this first case, we have a synthetic IncludedBuildCoordinates that really points to a local project in the
      // same (main) build.
      return if (gav == includedBuildId) {
        val gradleVariantIdentification = GradleVariantIdentification(
          capabilities = setOf(syntheticCapability),
          attributes = emptyMap(),
        )

        IncludedBuildCoordinates(
          identifier = includedBuildId,
          resolvedProject = ProjectCoordinates(
            identifier = requestedId,
            gradleVariantIdentification = gradleVariantIdentification,
            buildPath = ":",
          ),
          gradleVariantIdentification = gradleVariantIdentification,
        )
      } else {
        val capabilities = if (syntheticCapability.isNotEmpty()) {
          setOf(syntheticCapability)
        } else {
          emptySet()
        }

        val coord = Coordinates.of(gav)
        coord.copy(
          gradleVariantIdentification = GradleVariantIdentification(
            capabilities = capabilities.ifEmpty { setOf(coord.identifier) },
            attributes = emptyMap(),
          )
        )
      }
    }

    private fun getUsageFor(coordinates: Coordinates): Set<Usage> {
      // First check regular dependencies
      return dependencyUsages.entries.find { entry ->
        CoordinatesString.of(entry.key).matches(coordinates)
      }?.value?.softSortedSet(Usage.BY_VARIANT)
      // Then check annotation processors
        ?: annotationProcessorUsages.entries.find { entry ->
          CoordinatesString.of(entry.key).matches(coordinates)
        }?.value?.softSortedSet(Usage.BY_VARIANT)
        // Will be empty for runtimeOnly dependencies (no detected usages)
        ?: emptySet()
    }

    /** Returns null if there is no advice for the given id. */
    private fun findAdviceIn(projectAdvice: ProjectAdvice): Set<Advice> {
      return projectAdvice.dependencyAdvice.filterToSet { advice ->
        val adviceGav = advice.coordinates.gav()
        val byGav = adviceGav == targetCoord.gav() || adviceGav == requestedCoord.gav()

        byGav || matchesByIdentifier(advice, targetCoord) || matchesByIdentifier(advice, requestedCoord)
      }
    }

    private fun matchesByIdentifier(advice: Advice, request: Coordinates): Boolean {
      if (request !is FlatCoordinates) return false
      return advice.coordinates.identifier == request.identifier
    }

    // TODO: I think for any target, there's only 0 or 1 trace?
    /** Find all bundle traces where the [BundleTrace.top] or [BundleTrace.bottom] is [targetCoord]. */
    private fun bundleTraces(): Set<BundleTrace> {
      return parameters.bundleTracesReport.fromJsonSet<BundleTrace>().filterToSet {
        it.top.gav() == targetCoord.gav() || it.bottom.gav() == targetCoord.gav()
      }
    }

    private fun wasFiltered(): Boolean {
      return unfilteredAdvice.any { unfiltered ->
        unfiltered !in finalAdvice
      }
    }

    internal companion object {
      internal fun findFilteredDependencyKey(dependencies: Set<Map.Entry<String, Any>>, requestedId: String): String? {
        val filteredKeys = LinkedHashSet<String>()
        for (entry in dependencies) {
          if (equalsKey(requestedId, entry)) {
            // for exact equal - return immediately
            return entry.key
          }
          if (matchesKey(requestedId, entry)) {
            filteredKeys.add(CoordinatesString.of(entry.key).fullGav())
          }
        }

        return if (filteredKeys.isEmpty()) {
          null
        } else if (filteredKeys.size == 1) {
          filteredKeys.iterator().next()
        } else {
          throw InvalidUserDataException(
            "Coordinates '$requestedId' matches more than 1 dependency " +
              "${filteredKeys.map { secondCoordinatesKeySegment(it) ?: it }}"
          )
        }
      }
    }
  }

  interface ExplainModuleAdviceParams : WorkParameters {
    val module: Property<String>
    val projectPath: Property<String>
    val unfilteredAdviceReport: RegularFileProperty
    val finalAdviceReport: RegularFileProperty
  }

  abstract class ExplainModuleAdviceAction : WorkAction<ExplainModuleAdviceParams> {

    private val logger = getLogger<ReasonTask>()

    private val projectPath = parameters.projectPath.get()
    private val module = parameters.module.get()
    private val unfilteredAndroidScore = parameters.unfilteredAdviceReport
      .fromJson<ProjectAdvice>()
      .moduleAdvice
      .filterIsInstance<AndroidScore>()
      .singleOrNull()
    private val finalAndroidScore = parameters.finalAdviceReport
      .fromJson<ProjectAdvice>()
      .moduleAdvice
      .filterIsInstance<AndroidScore>()
      .singleOrNull()

    override fun execute() {
      validateModuleOption()
      val reason = ModuleAdviceExplainer(
        project = ProjectCoordinates(projectPath, GradleVariantIdentification.EMPTY),
        unfilteredAndroidScore = unfilteredAndroidScore,
        finalAndroidScore = finalAndroidScore,
      ).computeReason()

      logger.quiet(reason)
    }

    private fun validateModuleOption() {
      if (module != "android") {
        throw InvalidUserDataException(
          "'$module' unexpected. The only valid option for '--module' at this time is 'android'."
        )
      }
    }
  }

  internal interface Explainer {
    fun computeReason(): String
  }
}
