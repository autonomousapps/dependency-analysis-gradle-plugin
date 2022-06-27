package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.graph.Graphs.shortestPath
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.Colors.colorize
import com.autonomousapps.model.*
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import com.autonomousapps.model.intermediates.BundleTrace
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.model.intermediates.Usage
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class ReasonTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Explain how a dependency is used"
  }

  @get:Input
  abstract val projectPath: Property<String>

  // Not really optional, but we want to handle validation ourselves, rather than let Gradle do it
  @get:Optional
  @get:Input
  @set:Option(
    option = "id",
    description = "The dependency you'd like to reason about (com.foo:bar:1.0 or :other:module)"
  )
  var id: String? = null

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
    workerExecutor.noIsolation().submit(Action::class.java) {
      id.set(this@ReasonTask.id)
      projectPath.set(this@ReasonTask.projectPath)
      dependencyUsageReport.set(this@ReasonTask.dependencyUsageReport)
      annotationProcessorUsageReport.set(this@ReasonTask.annotationProcessorUsageReport)
      unfilteredAdviceReport.set(this@ReasonTask.unfilteredAdviceReport)
      finalAdviceReport.set(this@ReasonTask.finalAdviceReport)
      bundleTracesReport.set(this@ReasonTask.bundleTracesReport)
      dependencyGraphViews.set(this@ReasonTask.dependencyGraphViews)
    }
  }

  interface Parameters : WorkParameters {
    val id: Property<String?>
    val projectPath: Property<String>
    val dependencyUsageReport: RegularFileProperty
    val annotationProcessorUsageReport: RegularFileProperty
    val unfilteredAdviceReport: RegularFileProperty
    val finalAdviceReport: RegularFileProperty
    val bundleTracesReport: RegularFileProperty
    val dependencyGraphViews: ListProperty<RegularFile>
  }

  abstract class Action : WorkAction<Parameters> {

    private val logger = getLogger<ReasonTask>()

    private val projectPath = parameters.projectPath.get()
    private val dependencyGraph = parameters.dependencyGraphViews.get()
      .map { it.fromJson<DependencyGraphView>() }
      .associateBy { "${it.name},${it.configurationName}" }
    private val dependencyUsages = parameters.dependencyUsageReport.fromJsonMapSet<String, Usage>()
    private val annotationProcessorUsages = parameters.annotationProcessorUsageReport.fromJsonMapSet<String, Usage>()
    private val unfilteredProjectAdvice = parameters.unfilteredAdviceReport.fromJson<ProjectAdvice>()
    private val finalProjectAdvice = parameters.finalAdviceReport.fromJson<ProjectAdvice>()

    // Derived, compute lazily
    private val finalAdvice by unsafeLazy { findAdviceIn(finalProjectAdvice) }
    private val coord by unsafeLazy { getRequestedCoordinates() }
    private val unfilteredAdvice by unsafeLazy { findAdviceIn(unfilteredProjectAdvice) }
    private val usages by unsafeLazy { getUsageFor(coord.gav()) }

    override fun execute() {
      val reason = DeepThought(
        project = ProjectCoordinates(projectPath),
        target = coord,
        usages = usages,
        advice = finalAdvice,
        dependencyGraph = dependencyGraph,
        bundleTraces = bundleTraces(),
        wasFiltered = wasFiltered()
      ).computeReason()

      logger.quiet(reason)
    }

    /** Returns the requested ID as [Coordinates], even if user passed in a prefix. */
    private fun getRequestedCoordinates(): Coordinates {
      val id: String = parameters.id.orNull ?: throw InvalidUserDataException(
        """
        You must call 'reason' with the `--id` option. For example:
          ./gradlew ${projectPath}:reason --id com.foo:bar:1.0
          ./gradlew ${projectPath}:reason --id :other:module
          
        For external dependencies, the version string is optional.
      """.trimIndent()
      )

      fun findInGraph(): String? = dependencyGraph.values.asSequence()
        .flatMap { it.nodes }
        .map { it.gav() }
        .find { gav ->
          gav == id || gav.startsWith(id)
        }

      // Guaranteed to find full GAV or throw
      val gav = dependencyUsages.entries.find(id::equalsKey)?.key
        ?: dependencyUsages.entries.find(id::startsWithKey)?.key
        ?: annotationProcessorUsages.entries.find(id::equalsKey)?.key
        ?: annotationProcessorUsages.entries.find(id::startsWithKey)?.key
        ?: findInGraph()
        ?: throw InvalidUserDataException("There is no dependency with coordinates '$id' in this project.")
      return Coordinates.of(gav)
    }

    private fun getUsageFor(id: String): Set<Usage> {
      return dependencyUsages.entries.find(id::equalsKey)?.value?.toSortedSet(Usage.BY_VARIANT)
        ?: annotationProcessorUsages.entries.find(id::equalsKey)?.value?.toSortedSet(Usage.BY_VARIANT)
        // Will be empty for runtimeOnly dependencies (no detected usages)
        ?: emptySet()
    }

    private fun findAdviceIn(projectAdvice: ProjectAdvice): Advice? {
      // Would be null if there is no advice for the given id.
      return projectAdvice.dependencyAdvice.find { it.coordinates.gav() == coord.gav() }
    }

    // TODO: I think for any target, there's only 0 or 1 trace?
    /** Find all bundle traces where the [BundleTrace.top] or [BundleTrace.bottom] is [coord]. */
    private fun bundleTraces(): Set<BundleTrace> =
      parameters.bundleTracesReport.fromJsonSet<BundleTrace>().filterToSet {
        it.top == coord || it.bottom == coord
      }

    private fun wasFiltered(): Boolean = finalAdvice == null && unfilteredAdvice != null
  }

  internal class DeepThought(
    private val project: ProjectCoordinates,
    private val target: Coordinates,
    private val usages: Set<Usage>,
    private val advice: Advice?,
    private val dependencyGraph: Map<String, DependencyGraphView>,
    private val bundleTraces: Set<BundleTrace>,
    private val wasFiltered: Boolean
  ) {

    fun computeReason() = buildString {
      // Header
      appendReproducibleNewLine()
      append(Colors.BOLD)
      appendReproducibleNewLine("-".repeat(40))
      append("You asked about the dependency '${target.gav()}'.")
      appendReproducibleNewLine(Colors.NORMAL)
      appendReproducibleNewLine(adviceText())
      append(Colors.BOLD)
      append("-".repeat(40))
      appendReproducibleNewLine(Colors.NORMAL)

      // Shortest path
      dependencyGraph.forEach { printGraph(it.value) }

      // Usages
      printUsages()
    }

    private val bundle = "bundle".colorize(Colors.BOLD)

    private fun adviceText(): String = when {
      advice == null -> {
        if (bundleTraces.isNotEmpty()) {
          when (val trace = findTrace() ?: error("There must be a match. Available traces: $bundleTraces")) {
            is BundleTrace.DeclaredParent -> {
              "There is no advice regarding this dependency. It was removed because it matched a $bundle rule for " +
                "${trace.parent.gav().colorize(Colors.BOLD)}, which is already declared."
            }
            is BundleTrace.UsedChild -> {
              "There is no advice regarding this dependency. It was removed because it matched a $bundle rule for " +
                "${trace.child.gav().colorize(Colors.BOLD)}, which is declared and used."
            }
            else -> error("Trace was $trace, which makes no sense in this context")
          }
        } else if (wasFiltered) {
          val exclude = "exclude".colorize(Colors.BOLD)
          "There is no advice regarding this dependency. It was removed because it matched an $exclude rule."
        } else {
          "There is no advice regarding this dependency."
        }
      }
      advice.isAdd() -> {
        val trace = findTrace()
        if (trace != null) {
          check(trace is BundleTrace.PrimaryMap) { "Expected a ${BundleTrace.PrimaryMap::class.java.simpleName}" }
          "You have been advised to add this dependency to '${advice.toConfiguration!!.colorize(Colors.GREEN)}'. " +
            "It matched a $bundle rule: ${trace.primary.gav().colorize(Colors.BOLD)} was substituted for " +
            "${trace.subordinate.gav().colorize(Colors.BOLD)}."
        } else {
          "You have been advised to add this dependency to '${advice.toConfiguration!!.colorize(Colors.GREEN)}'."
        }
      }
      advice.isRemove() || advice.isProcessor() -> {
        "You have been advised to remove this dependency from '${advice.fromConfiguration!!.colorize(Colors.RED)}'."
      }
      advice.isChange() || advice.isRuntimeOnly() || advice.isCompileOnly() -> {
        "You have been advised to change this dependency to '${advice.toConfiguration!!.colorize(Colors.GREEN)}' " +
          "from '${advice.fromConfiguration!!.colorize(Colors.YELLOW)}'."
      }
      else -> error("Unknown advice type: $advice")
    }

    // TODO: what are the valid scenarios? How many traces could there be for a single target?
    private fun findTrace(): BundleTrace? = bundleTraces.find { it.top == target || it.bottom == target }

    private fun StringBuilder.printGraph(graphView: DependencyGraphView) {
      val name = graphView.configurationName

      val nodes = graphView.graph.shortestPath(source = project, target = target)
      if (!nodes.iterator().hasNext()) {
        appendReproducibleNewLine()
        append(Colors.BOLD)
        appendReproducibleNewLine("There is no path from ${project.printableName()} to ${target.gav()} for $name")
        appendReproducibleNewLine(Colors.NORMAL)
        return
      }

      appendReproducibleNewLine()
      append(Colors.BOLD)
      append("Shortest path from ${project.printableName()} to ${target.gav()} for $name:")
      appendReproducibleNewLine(Colors.NORMAL)
      appendReproducibleNewLine(project.gav())
      nodes.drop(1).forEachIndexed { i, node ->
        append("      ".repeat(i))
        append("\\--- ")
        appendReproducibleNewLine(node.gav())
      }
    }

    private fun StringBuilder.printUsages() {
      if (usages.isEmpty()) {
        appendReproducibleNewLine()
        appendReproducibleNewLine("No compile-time usages detected for this runtime-only dependency.")
        return
      }

      usages.forEach { usage ->
        val variant = usage.variant

        appendReproducibleNewLine()
        sourceText(variant).let { txt ->
          append(Colors.BOLD)
          appendReproducibleNewLine(txt)
          append("-".repeat(txt.length))
          appendReproducibleNewLine(Colors.NORMAL)
        }

        val reasons = usage.reasons.filter { it !is Reason.Unused && it !is Reason.Undeclared }
        val isCompileOnly = reasons.any { it is Reason.CompileTimeAnnotations }
        reasons.forEach { reason ->
          append("""* """)
          val prefix = if (variant.kind == SourceSetKind.MAIN) "" else "test"
          appendReproducibleNewLine(reason.reason(prefix, isCompileOnly))
        }
        if (reasons.isEmpty()) {
          appendReproducibleNewLine("(no usages)")
        }
      }
    }

    private fun ProjectCoordinates.printableName(): String {
      val gav = gav()
      return if (gav == ":") "root project" else gav
    }

    private fun sourceText(variant: Variant): String = when (variant.variant) {
      Variant.MAIN_NAME, Variant.TEST_NAME -> "Source: ${variant.variant}"
      else -> "Source: ${variant.variant}, ${variant.kind.name.lowercase()}"
    }
  }
}

private fun <T> String.equalsKey(mapEntry: Map.Entry<String, T>) = mapEntry.key == this
private fun <T> String.startsWithKey(mapEntry: Map.Entry<String, T>) = mapEntry.key.startsWith(this)
