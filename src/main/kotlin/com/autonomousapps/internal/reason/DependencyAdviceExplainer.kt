// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.reason

import com.autonomousapps.graph.Graphs.shortestPath
import com.autonomousapps.internal.utils.Colors
import com.autonomousapps.internal.utils.Colors.colorize
import com.autonomousapps.internal.utils.appendReproducibleNewLine
import com.autonomousapps.internal.utils.lowercase
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.intermediates.BundleTrace
import com.autonomousapps.model.internal.intermediates.Reason
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.tasks.ReasonTask

@Suppress("UnstableApiUsage") // guava
internal class DependencyAdviceExplainer(
  private val project: ProjectCoordinates,
  private val requested: Coordinates,
  private val target: Coordinates,
  private val requestedCapability: String,
  private val usages: Set<Usage>,
  private val advice: Set<Advice>,
  private val dependencyGraph: Map<String, DependencyGraphView>,
  private val bundleTraces: Set<BundleTrace>,
  private val wasFiltered: Boolean,
  private val dependencyMap: ((String) -> String?)? = null
) : ReasonTask.Explainer {

  private val ruleLength = 60

  override fun computeReason() = buildString {
    // Header
    appendReproducibleNewLine()
    append(Colors.BOLD)
    appendReproducibleNewLine("-".repeat(ruleLength))

    append("You asked about the dependency '${printableIdentifier(requested)}'")
    if (requestedCapability.isNotBlank()) {
      append(", with the capability '$requestedCapability'")
    }
    append(".")

    appendReproducibleNewLine(Colors.NORMAL)
    appendReproducibleNewLine(adviceText())
    append(Colors.BOLD)
    append("-".repeat(ruleLength))
    appendReproducibleNewLine(Colors.NORMAL)

    // Shortest path
    dependencyGraph.forEach { printGraph(it.value) }

    // Usages
    printUsages()
  }

  private val bundle = "bundle".colorize(Colors.BOLD)

  private fun adviceText(): String {
    if (advice.isEmpty()) {
      return if (bundleTraces.isNotEmpty()) {
        when (val trace = findTrace() ?: error("There must be a match. Available traces: $bundleTraces")) {
          is BundleTrace.DeclaredParent -> {
            "There is no advice regarding this dependency.\nIt was removed because it matched a $bundle rule for " +
              "${printableIdentifier(trace.parent).colorize(Colors.BOLD)}, which is already declared."
          }

          is BundleTrace.UsedChild -> {
            "There is no advice regarding this dependency.\nIt was removed because it matched a $bundle rule for " +
              "${printableIdentifier(trace.child).colorize(Colors.BOLD)}, which is declared and used."
          }

          is BundleTrace.PrimaryMap -> {
            "There is no advice regarding this dependency.\nIt was removed because it matched a $bundle rule for " +
              "${printableIdentifier(trace.primary).colorize(Colors.BOLD)}, which is already present in the dependency graph."
          }
        }
      } else if (wasFiltered) {
        val exclude = "exclude".colorize(Colors.BOLD)
        "There is no advice regarding this dependency.\nIt was removed because it matched an $exclude rule."
      } else {
        "There is no advice regarding this dependency."
      }
    }

    val builder = StringBuilder()

    advice.forEach { a ->
      val text = when {
        a.isAdd() -> {
          val trace = findTrace()
          if (trace != null) {
            check(trace is BundleTrace.PrimaryMap) { "Expected a ${BundleTrace.PrimaryMap::class.java.simpleName}" }

            "You have been advised to add this dependency to '${a.toConfiguration!!.colorize(Colors.GREEN)}'.\n" +
              "It matched a $bundle rule: ${printableIdentifier(trace.primary).colorize(Colors.BOLD)} was substituted for " +
              "${printableIdentifier(trace.subordinate).colorize(Colors.BOLD)}."
          } else {
            "You have been advised to add this dependency to '${a.toConfiguration!!.colorize(Colors.GREEN)}'."
          }
        }

        a.isRemove() || a.isProcessor() -> {
          "You have been advised to remove this dependency from '${a.fromConfiguration!!.colorize(Colors.RED)}'."
        }

        a.isChange() || a.isRuntimeOnly() || a.isCompileOnly() -> {
          "You have been advised to change this dependency to '${a.toConfiguration!!.colorize(Colors.GREEN)}' " +
            "from '${a.fromConfiguration!!.colorize(Colors.YELLOW)}'."
        }

        else -> error("Unknown advice type: $advice")
      }

      if (builder.isNotEmpty()) {
        // If we've already added some text, we need to insert a newline before adding more.
        builder.appendLine()
      }
      builder.append(text)
    }

    return builder.toString()
  }

  // TODO(tsr): what are the valid scenarios? How many traces could there be for a single target?
  private fun findTrace(): BundleTrace? = bundleTraces.find { trace ->
    trace.top.gav() == target.gav() || trace.bottom.gav() == target.gav()
  }

  private fun StringBuilder.printGraph(graphView: DependencyGraphView) {
    val name = graphView.configurationName

    // Find the complete Coordinates (including variant identification) in the graph (if available)
    val targetInGraph = graphView.graph.nodes().firstOrNull { coordinates ->
      coordinates.identifier == target.identifier && matchesTargetCapabilities(coordinates)
    }

    if (targetInGraph == null) {
      appendReproducibleNewLine()
      append(Colors.BOLD)
      appendReproducibleNewLine(
        "There is no path from ${project.printableName()} to ${printableIdentifier(target)} for $name"
      )
      appendReproducibleNewLine(Colors.NORMAL)
      return
    }

    val nodes = graphView.graph.shortestPath(source = project, target = targetInGraph)

    appendReproducibleNewLine()
    append(Colors.BOLD)
    // append("Shortest path from ${project.printableName()} to ${printableIdentifier(target)} for $name:")
    append("Shortest path from ${project.printableName()} to ${printableIdentifier(requested)} for $name:")
    appendReproducibleNewLine(Colors.NORMAL)
    appendReproducibleNewLine(project.gav())

    nodes.drop(1).forEachIndexed { i, node ->
      append("      ".repeat(i))
      append("\\--- ")
      append(humanReadableGav(node))
      printCapabilities(node)
      appendReproducibleNewLine()
    }
  }

  private fun matchesTargetCapabilities(coordinates: Coordinates): Boolean {
    // If their GVIs exactly match
    return coordinates.gradleVariantIdentification == target.gradleVariantIdentification
      // Or if the target isn't requesting on a capability and the coordinates have only the default capability.
      || (target.gradleVariantIdentification.capabilities.isEmpty() && coordinates.hasDefaultCapability())
  }

  private fun StringBuilder.printCapabilities(node: Coordinates) {
    val capabilities = node.gradleVariantIdentification.capabilities.filterNot { it == node.identifier }

    if (capabilities.isNotEmpty()) {
      val capabilityString = capabilities.map { it.removePrefix("${node.identifier}-") }
      append(" (capabilities: $capabilityString)")
    }
  }

  private fun StringBuilder.printUsages() {
    if (usages.isEmpty()) {
      appendReproducibleNewLine()
      appendReproducibleNewLine("No compile-time usages detected for this runtime-only dependency.")
      return
    }

    usages.forEach { usage ->
      val variant = usage.sourceKind

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
        val prefix = when (variant.kind) {
          SourceKind.MAIN_KIND -> ""
          SourceKind.CUSTOM_JVM_KIND -> variant.kind
          else -> "test"
        }
        appendReproducibleNewLine(reason.reason(prefix, isCompileOnly))
      }
      if (reasons.isEmpty()) {
        appendReproducibleNewLine("(no usages)")
      }
    }
  }

  /**
   * Returns a human-readable identifier for [coordinates], taking into account plugin- and user-supplied mappings, e.g.
   * to handle version catalog accessors.
   */
  private fun printableIdentifier(coordinates: Coordinates): String {
    val gav = humanReadableGav(coordinates)
    val mapped = dependencyMap?.invoke(gav) ?: dependencyMap?.invoke(coordinates.identifier)

    return if (!mapped.isNullOrBlank()) "$gav ($mapped)" else gav
  }

  /** Strip the root project name prefix off of "included build" dependencies for more readable output. */
  private fun humanReadableGav(coordinates: Coordinates): String {
    return if (coordinates is IncludedBuildCoordinates) {
      if (coordinates.resolvedProject.buildPath == project.buildPath) {
        coordinates.resolvedProject.identifier
      } else {
        coordinates.gav()
      }
    } else {
      coordinates.gav()
    }
  }

  private fun ProjectCoordinates.printableName(): String {
    val gav = gav()
    return if (gav == ":") "root project" else gav
  }

  private fun sourceText(variant: SourceKind): String = when {
    variant.name in listOf(SourceKind.MAIN_NAME, SourceKind.TEST_NAME)
      || variant.kind == SourceKind.CUSTOM_JVM_KIND -> "Source: ${variant.name}"

    // Android, I think:
    else -> "Source: ${variant.name}, ${variant.kind.lowercase()}"
  }
}
