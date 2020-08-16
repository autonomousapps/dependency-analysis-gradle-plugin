package com.autonomousapps.graph

import com.autonomousapps.advice.Advice

internal sealed class Reason(queryNode: ProducerNode) {

  protected val headerText = "You asked about the dependency ${queryNode.identifier}."

  abstract val path: Iterable<Edge>

  companion object {
    fun determine(
      graph: DependencyGraph,
      queryNode: ProducerNode,
      advice: List<Advice>
    ): Reason {

      val theAdvice = advice.find {
        it.dependency.identifier == queryNode.identifier
      }

      return when {
        theAdvice == null -> NoReason(queryNode)
        theAdvice.isRemove() || theAdvice.isProcessor() -> RemoveReason(
          advice = theAdvice,
          queryNode = queryNode,
          graph = graph
        )
        theAdvice.isAdd() -> AddReason(
          advice = theAdvice,
          queryNode = queryNode,
          graph = graph
        )
        theAdvice.isChange() || theAdvice.isCompileOnly() -> ChangeReason(
          graph = graph,
          queryNode = queryNode,
          advice = theAdvice
        )
        else -> error("No known reason for $theAdvice.")
      }
    }
  }
}

// TODO is it worth indicating which facilities are being used when there's no advice?
private class NoReason(queryNode: ProducerNode) : Reason(queryNode) {
  override fun toString() = "$headerText There is no advice regarding this dependency."
  override val path: Iterable<Edge> = emptyList()
}

/**
 * For advice to remove a dependency or remove an annotation processor.
 */
private class RemoveReason(
  advice: Advice,
  queryNode: ProducerNode,
  graph: DependencyGraph
) : Reason(queryNode) {

  private val root = graph.rootNode
  private val sp = ShortestPath(graph, root)
  override val path = sp.pathTo(queryNode)
    ?: error("No path to node $queryNode, violating invariant")

  private val adviceText = "You have been advised to remove this dependency."
  private val pathText = PathPrinter.printPath(path)
  private val infoText = NodePrinter.print(queryNode)
  private val coda = when {
    advice.isRemove() -> "And none of these are used."
    advice.isProcessor() -> "And this annotation processor is not used."
    else -> error("a remove-reason must be either isRemove or isProcessor")
  }

  override fun toString() = "$headerText $adviceText\n\n$pathText\n$infoText\n$coda"
}

/**
 * For advice to declare directly a used-transitive dependency.
 */
private class AddReason(
  private val advice: Advice,
  private val queryNode: ProducerNode,
  graph: DependencyGraph
) : Reason(queryNode) {

  private val root = graph.rootNode
  private val sp = ShortestPath(graph, root)
  override val path = sp.pathTo(queryNode)
    ?: error("No path to node $queryNode, violating invariant")

  private val adviceText = "You have been advised to add this dependency."
  private val pathText = PathPrinter.printPath(path)
  private val infoText = NodePrinter.print(queryNode)
  private val coda = makeCoda()

  override fun toString() = "$headerText $adviceText\n\n$pathText\n$infoText\n$coda"

  private fun makeCoda(): String =
    if (Change.which(advice.toConfiguration!!) == Change.TO_API) {
      val publicClasses = queryNode.reasonableDependency
        ?.publicClasses
        ?.joinToString(prefix = "- ", separator = "\n- ")
        ?: error("No public component found matching ${queryNode.identifier}")

      "And this project exposes the following class(es) provided by this dependency:\n$publicClasses\n\nPlease see abi-dump.txt for more information."
    } else {
      val usedClasses = queryNode.reasonableDependency
        ?.usedTransitiveClasses
        ?.joinToString(prefix = "- ", separator = "\n- ")
        ?: error("Missing ReasonableDependency for ${queryNode.identifier}")

      "And this project uses the following classes provided by this dependency:\n$usedClasses\n"
    }
}

/**
 * For advice to change how a dependency is declared (such as from api to implementation, or vice
 * versa).
 */
private class ChangeReason(
  graph: DependencyGraph,
  queryNode: ProducerNode,
  advice: Advice
) : Reason(queryNode) {

  private val root = graph.rootNode
  private val sp = ShortestPath(graph, root)
  override val path = sp.pathTo(queryNode)
    ?: error("No path to node $queryNode, violating invariant")

  private val adviceText = "You have been advised to change this dependency to ${advice.toConfiguration} from ${advice.fromConfiguration}."
  private val pathText = PathPrinter.printPath(path)
  private val infoText = NodePrinter.print(queryNode)
  private val coda = when {
    advice.isChange() -> Change.which(advice.toConfiguration!!).reason
    advice.isCompileOnly() -> "And this dependency is only required during compilation."
    else -> error("a change-reason must be either isChange or isCompileOnly")
  }

  override fun toString() = "$headerText $adviceText\n\n$pathText\n$infoText\n$coda"
}

private enum class Change(val reason: String) {
  TO_IMPL("And this dependency is not exposed as part of this project's ABI."),
  TO_API("And this dependency is exposed as part of this project's ABI.");

  companion object {
    fun which(toConf: String): Change = when {
      toConf.endsWith("implementation", ignoreCase = true) -> TO_IMPL
      toConf.endsWith("api", ignoreCase = true) -> TO_API
      else -> error("which not yet implemented for $toConf")
    }
  }
}
