package com.autonomousapps.internal.reason

import com.autonomousapps.internal.graph.Graphs.shortestPath
import com.autonomousapps.internal.utils.Colors
import com.autonomousapps.internal.utils.Colors.colorize
import com.autonomousapps.internal.utils.lowercase
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import com.autonomousapps.model.intermediates.BundleTrace
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.model.intermediates.Usage
import com.autonomousapps.tasks.ReasonTask
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

internal class DependencyAdviceExplainer(
  private val project: ProjectCoordinates,
  private val target: Coordinates,
  private val usages: Set<Usage>,
  private val advice: Advice?,
  private val dependencyGraph: Map<String, DependencyGraphView>,
  private val bundleTraces: Set<BundleTrace>,
  private val wasFiltered: Boolean,
  private val dependencyMap: (String) -> String = { it }
) : ReasonTask.Explainer {

  override fun computeReason() = buildString {
    // Header
    appendReproducibleNewLine()
    append(Colors.BOLD)
    appendReproducibleNewLine("-".repeat(40))
    append("You asked about the dependency '${printableIdentifier(target)}'.")
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
              "${printableIdentifier(trace.parent).colorize(Colors.BOLD)}, which is already declared."
          }
          is BundleTrace.UsedChild -> {
            "There is no advice regarding this dependency. It was removed because it matched a $bundle rule for " +
              "${printableIdentifier(trace.child).colorize(Colors.BOLD)}, which is declared and used."
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
          "It matched a $bundle rule: ${printableIdentifier(trace.primary).colorize(Colors.BOLD)} was substituted for " +
          "${printableIdentifier(trace.subordinate).colorize(Colors.BOLD)}."
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
      appendReproducibleNewLine(
        "There is no path from ${project.printableName()} to ${printableIdentifier(target)} for $name"
      )
      appendReproducibleNewLine(Colors.NORMAL)
      return
    }

    appendReproducibleNewLine()
    append(Colors.BOLD)
    append("Shortest path from ${project.printableName()} to ${printableIdentifier(target)} for $name:")
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
        if (reason is Reason.Impl && reason.extraInfo.isNotEmpty()) {
          appendReproducibleNewLine(reason.extraInfo)
        }
      }
      if (reasons.isEmpty()) {
        appendReproducibleNewLine("(no usages)")
      }
    }
  }

  private fun printableIdentifier(coordinates: Coordinates): String {
    val gav = coordinates.gav()
    val mapped = dependencyMap(gav)
    return if (gav == mapped) "$gav" else "$gav ($mapped)"
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
