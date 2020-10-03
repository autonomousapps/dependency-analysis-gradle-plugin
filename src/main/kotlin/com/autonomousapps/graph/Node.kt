package com.autonomousapps.graph

import com.autonomousapps.advice.ReasonableDependency
import com.autonomousapps.internal.Manifest

/**
 * Represents a module in the dependency hierarchy rooted on the project-under-analysis (PUA). May
 * be a [ConsumerNode] (i.e., the PUA), or a [ProducerNode] (i.e. a dependency).
 */
internal sealed class Node(
  open val identifier: String
) {

  override fun toString(): String = identifier

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Node

    if (identifier != other.identifier) return false

    return true
  }

  override fun hashCode(): Int {
    return identifier.hashCode()
  }
}

/**
 * The project under analysis. It "consumes" its dependencies.
 */
internal data class ConsumerNode(
  override val identifier: String,
  val classes: Set<String>? = null
) : Node(identifier)

/**
 * A dependency. May be a project or an external binary. It "provides" facilities for use by the
 * project-under-analysis, represented by [ConsumerNode].
 */
internal data class ProducerNode(
  override val identifier: String,
  val reasonableDependency: ReasonableDependency? = null
) : Node(identifier)

internal object NodePrinter {

  fun print(node: Node): String = when (node) {
    is ConsumerNode -> printConsumer(node)
    is ProducerNode -> printProducer(node)
  }

  private fun printConsumer(node: ConsumerNode) = buildString {
    append("what are you doing")
  }

  private fun printProducer(node: ProducerNode) = buildString {
    val reasonableDependency = node.reasonableDependency ?: error("")
    val constantCount = reasonableDependency.constantFields
      ?.map { it.value.size }
      ?.fold(0) { acc, i -> acc + i }
      ?: 0

    append("Dependency ${node.identifier} provides the following:\n")
    append("- ${reasonableDependency.classes.size} classes\n") // TODO only public, or all?
    append("- $constantCount public constants\n")
    if (reasonableDependency.isCompileOnly) append("- is a compileOnly candidate\n")
    if (reasonableDependency.isSecurityProvider) append("- is a security provider\n")
    if (reasonableDependency.providesInlineMembers == true) append("- provides inline functions\n")
    if (reasonableDependency.manifestComponents?.isNotEmpty() == true) {
      append("- provides these Android manifest components:\n")
      append(manifestComponents(reasonableDependency.manifestComponents))
    }
    if (reasonableDependency.providesNativeLibs == true) append("- provides native libraries\n")
  }

  private fun manifestComponents(components: Map<String, Set<String>>): String {
    if (components.isEmpty()) error("Expected non-empty component map")

    val activities = components[Manifest.Component.ACTIVITY.mapKey]
    val services = components[Manifest.Component.SERVICE.mapKey]
    val providers = components[Manifest.Component.PROVIDER.mapKey]
    val receivers = components[Manifest.Component.RECEIVER.mapKey]

    val builder = StringBuilder()
    if (activities?.isNotEmpty() == true) {
      builder.append("  - Activities:\n")
      builder.append(activities.joinToString(prefix = "    - ", separator = "\n    - ") { it })
    }
    if (services?.isNotEmpty() == true) {
      builder.append("  - Services:\n")
      builder.append(services.joinToString(prefix = "    - ", separator = "\n    - ") { it })
    }
    if (providers?.isNotEmpty() == true) {
      builder.append("  - Providers:\n")
      builder.append(providers.joinToString(prefix = "    - ", separator = "\n    - ") { it })
    }
    if (receivers?.isNotEmpty() == true) {
      builder.append("  - Receivers:\n")
      builder.append(receivers.joinToString(prefix = "    - ", separator = "\n    - ") { it })
    }
    return builder.toString()
  }
}

internal object PathPrinter {
  fun printPath(path: Iterable<Edge>) = buildString {
    val rev = path.reversed()
    val last = rev.last()
    append("Shortest path to ${last.to.identifier} from the current project:\n")

    var level = 0
    rev.forEach { edge ->
      append("${indent(level++)}${edge.from.identifier}\n")
    }
    append("${indent(level)}${last.to.identifier}\n")
  }

  private fun indent(level: Int): String =
    if (level == 0) ""
    else "     ".repeat(level - 1) + "\\--- "
}
