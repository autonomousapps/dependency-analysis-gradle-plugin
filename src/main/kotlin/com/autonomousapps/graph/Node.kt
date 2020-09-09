package com.autonomousapps.graph

import com.autonomousapps.advice.ReasonableDependency

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
      .map { it.value.size }
      .fold(0) { acc, i -> acc + i }

    append("Dependency ${node.identifier} provides the following:\n")
    append("- ${reasonableDependency.classes.size} classes\n") // TODO only public, or all?
    append("- $constantCount public constants\n")
    if (reasonableDependency.isCompileOnly) append("- is a compileOnly candidate\n")
    if (reasonableDependency.isSecurityProvider) append("- is a security provider\n")
    if (reasonableDependency.providesInlineMembers == true) append("- provides inline functions\n")
    if (reasonableDependency.providesManifestComponents == true) append("- provides Android manifest components\n")
    if (reasonableDependency.providesNativeLibs == true) append("- provides native libraries\n")
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
