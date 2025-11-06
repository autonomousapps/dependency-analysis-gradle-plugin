// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.internal.UsageContainer
import com.autonomousapps.internal.reason.DependencyAdviceExplainer
import com.autonomousapps.internal.reason.ModuleAdviceExplainer
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.*
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

// TODO(tsr): probably need a "ComputeReasonTask" and a "PrintReasonTask"
@UntrackedTask(because = "Always prints output")
public abstract class ReasonTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Explain how a dependency is used"
  }

  @get:Input
  public abstract val projectPath: Property<String>

  @get:Input
  public abstract val buildPath: Property<String>

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
  public var id: String? = null

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
  public var capability: String? = null

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
  public var module: String? = null

  @get:Input
  public abstract val dependencyMap: MapProperty<String, String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val dependencyUsageReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val annotationProcessorUsageReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val unfilteredAdviceReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val finalAdviceReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val bundleTracesReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val dependencyGraphViews: ListProperty<RegularFile>

  // TODO InputDirectory of all dependencies for finding capabilities

  @TaskAction public fun action() {
    val options = options()

    // Explain dependency advice
    options.id?.let { dependency ->
      workerExecutor.noIsolation().submit(ExplainDependencyAdviceAction::class.java) {
        it.id.set(dependency)
        it.capability.set(options.capability ?: "")
        it.projectPath.set(projectPath)
        it.buildPath.set(buildPath)
        it.dependencyMap.set(dependencyMap)
        it.dependencyUsageReport.set(dependencyUsageReport)
        it.annotationProcessorUsageReport.set(annotationProcessorUsageReport)
        it.unfilteredAdviceReport.set(unfilteredAdviceReport)
        it.finalAdviceReport.set(finalAdviceReport)
        it.bundleTracesReport.set(bundleTracesReport)
        it.dependencyGraphViews.set(dependencyGraphViews)
      }
    }

    // Explain module structure advice
    options.module?.let { moduleStructure ->
      workerExecutor.noIsolation().submit(ExplainModuleAdviceAction::class.java) {
        it.module.set(moduleStructure)
        it.projectPath.set(projectPath)
        it.unfilteredAdviceReport.set(unfilteredAdviceReport)
        it.finalAdviceReport.set(finalAdviceReport)
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

  public interface ExplainDependencyAdviceParams : WorkParameters {
    public val id: Property<String>
    public val capability: Property<String>
    public val projectPath: Property<String>
    public val buildPath: Property<String>
    public val dependencyMap: MapProperty<String, String>
    public val dependencyUsageReport: RegularFileProperty
    public val annotationProcessorUsageReport: RegularFileProperty
    public val unfilteredAdviceReport: RegularFileProperty
    public val finalAdviceReport: RegularFileProperty
    public val bundleTracesReport: RegularFileProperty
    public val dependencyGraphViews: ListProperty<RegularFile>
  }

  public abstract class ExplainDependencyAdviceAction : WorkAction<ExplainDependencyAdviceParams> {

    private val logger = getLogger<ReasonTask>()

    private val capability = parameters.capability.get()
    private val buildPath = parameters.buildPath.get()
    private val projectPath = parameters.projectPath.get()
    private val dependencyGraph = parameters.dependencyGraphViews.get()
      .map { it.fromJson<DependencyGraphView>() }
      .associateBy { "${it.name},${it.configurationName}" }
    private val unfilteredProjectAdvice = parameters.unfilteredAdviceReport.fromJson<ProjectAdvice>()
    private val finalProjectAdvice = parameters.finalAdviceReport.fromJson<ProjectAdvice>()
    private val dependencyMap = parameters.dependencyMap.get().toLambda()

    private val dependencyUsages = parameters.dependencyUsageReport.fromJson<UsageContainer>().toMap()
    private val annotationProcessorUsages = parameters.annotationProcessorUsageReport.fromJson<UsageContainer>().toMap()

    // Derived from the above
    private val requestedCoord = getRequestedCoordinates()
    private val finalAdvice = findAdviceIn(finalProjectAdvice)
    private val unfilteredAdvice = findAdviceIn(unfilteredProjectAdvice)
    private val usages = getUsageFor(requestedCoord)

    override fun execute() {
      val project = ProjectCoordinates(
        identifier = projectPath,
        gradleVariantIdentification = GradleVariantIdentification(
          capabilities = setOf("ROOT"),
          attributes = emptyMap(),
        ),
        buildPath = buildPath,
      )

      val reason = DependencyAdviceExplainer(
        project = project,
        buildPath = buildPath,
        requested = requestedCoord,
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
    private fun getRequestedCoordinates(): Coordinates {
      val requestedId = parameters.id.get()
      val requestedCapability = capability

      fun findInGraph(): Coordinates? = dependencyGraph.values.asSequence()
        .flatMap { it.nodes }
        .find { coordinates ->
          val gav = coordinates.gav()
          gav == requestedId
            || gav.startsWith("$requestedId:")
            || dependencyMap(gav) == requestedId
            || dependencyMap(coordinates.identifier) == requestedId
        }

      // Guaranteed to find full GAV or throw
      return findDependency(dependencyUsages.entries, requestedId, requestedCapability, buildPath)
        ?: findDependency(annotationProcessorUsages.entries, requestedId, requestedCapability, buildPath)
        ?: findInGraph()
        ?: throw InvalidUserDataException("There is no dependency with coordinates '$requestedId' in this project.")
    }

    private fun getUsageFor(request: Coordinates): Set<Usage> {
      // First check regular dependencies
      return dependencyUsages.entries.find { entry ->
        request == entry.key.normalized(buildPath)
      }?.value?.softSortedSet(Usage.BY_VARIANT)
      // Then check annotation processors
        ?: annotationProcessorUsages.entries.find { entry ->
          request == entry.key.normalized(buildPath)
        }?.value?.softSortedSet(Usage.BY_VARIANT)
        // Will be empty for runtimeOnly dependencies (no detected usages)
        ?: emptySet()
    }

    /** Returns null if there is no advice for the given id. */
    private fun findAdviceIn(projectAdvice: ProjectAdvice): Set<Advice> {
      return projectAdvice.dependencyAdvice.filterToSet { advice ->
        val adviceGav = advice.coordinates.gav()
        val byGav = adviceGav == requestedCoord.gav() || adviceGav == requestedCoord.gav()

        byGav || matchesByIdentifier(advice, requestedCoord) || matchesByIdentifier(advice, requestedCoord)
      }
    }

    private fun matchesByIdentifier(advice: Advice, request: Coordinates): Boolean {
      if (request !is FlatCoordinates) return false
      return advice.coordinates.identifier == request.identifier
    }

    // TODO: I think for any target, there's only 0 or 1 trace?
    /** Find all bundle traces where the [BundleTrace.top] or [BundleTrace.bottom] is [requestedCoord]. */
    private fun bundleTraces(): Set<BundleTrace> {
      return parameters.bundleTracesReport.fromJsonSet<BundleTrace>().filterToSet {
        it.top.gav() == requestedCoord.gav() || it.bottom.gav() == requestedCoord.gav()
      }
    }

    private fun wasFiltered(): Boolean {
      return unfilteredAdvice.any { unfiltered ->
        unfiltered !in finalAdvice
      }
    }

    internal companion object {
      internal fun findDependency(
        dependencies: Set<Map.Entry<Coordinates, Set<Usage>>>,
        requestedId: String,
        requestedCapability: String,
        buildPath: String,
      ): Coordinates? {
        val filteredKeys = LinkedHashSet<Coordinates>()
        for (entry in dependencies) {
          val coordinates = entry.key
          val normalizedCoordinates = coordinates.normalized(buildPath)

          val byGav = requestedId == normalizedCoordinates.gav()
          val byCapability = (requestedCapability.isBlank() && coordinates.hasDefaultCapability())
            || coordinates.gradleVariantIdentification.capabilities.any { it.endsWith(requestedCapability) }

          if (byGav && byCapability) {
            // for exact equal - return immediately
            return normalizedCoordinates
          }
          if (requestedId == normalizedCoordinates.identifier || normalizedCoordinates.gav().startsWith(requestedId)) {
            filteredKeys.add(normalizedCoordinates)
          }
        }

        return if (filteredKeys.isEmpty()) {
          null
        } else if (filteredKeys.size == 1) {
          filteredKeys.iterator().next()
        } else {
          throw InvalidUserDataException(
            "Coordinates '$requestedId' matches more than 1 dependency: ${filteredKeys.map(Coordinates::gav)}"
          )
        }
      }
    }
  }

  public interface ExplainModuleAdviceParams : WorkParameters {
    public val module: Property<String>
    public val projectPath: Property<String>
    public val unfilteredAdviceReport: RegularFileProperty
    public val finalAdviceReport: RegularFileProperty
  }

  public abstract class ExplainModuleAdviceAction : WorkAction<ExplainModuleAdviceParams> {

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
