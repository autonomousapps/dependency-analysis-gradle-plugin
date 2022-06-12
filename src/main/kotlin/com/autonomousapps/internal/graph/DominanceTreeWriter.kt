package com.autonomousapps.internal.graph

import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

@Suppress("UnstableApiUsage") // Guava
internal class DominanceTreeWriter<N : Any>(
  private val root: N,
  private val tree: DominanceTree<N>,
  private val nodeWriter: NodeWriter<N>,
) {

  private val builder = StringBuilder()
  val string: String get() = builder.toString()

  init {
    compute()
  }

  private fun compute() {
    val visiting = linkedMapOf<N, MutableSet<N>>()

    // start by printing root node
    builder.appendReproducibleNewLine(nodeWriter.toString(root))

    fun dfs(node: N) {
      val subs = tree.dominanceGraph.successors(node).run { nodeWriter.comparator()?.let { sortedWith(it) } ?: this }
      visiting[node] = subs.toMutableSet()

      subs.forEach { sub ->
        visiting.forEach { (_, subs) ->
          if (subs.contains(sub)) {
            // SLASH or PLUS
            if (subs.size == 1) {
              builder.append(SLASH)
            } else {
              builder.append(PLUS)
            }
          } else {
            // TAB or PIPE
            if (subs.size == 1) {
              builder.append(TAB)
            } else {
              builder.append(PIPE)
            }
          }
        }

        builder.appendReproducibleNewLine(nodeWriter.toString(sub))

        dfs(sub)

        visiting[node]!!.remove(sub)
      }
      visiting.remove(node)
    }

    dfs(root)
  }

  internal interface NodeWriter<N : Any> {
    /** A [Comparator] for sorting nodes of type [N]. May be null if you don't care about print order. */
    fun comparator(): Comparator<N>?

    /** String representation of [node] that will be printed to console. */
    fun toString(node: N): String
  }

  private companion object {
    private const val SLASH = """\--- """
    private const val PLUS = "+--- "
    private const val TAB = "     "
    private const val PIPE = "|    "
  }
}
