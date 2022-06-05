package com.autonomousapps.internal.graph

import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

@Suppress("UnstableApiUsage") // Guava
internal class DominanceTreeWriter<N : Any>(
  private val root: N,
  private val tree: DominanceTree<N>,
  private val prefixer: Prefixer<N>,
  private val stringify: (N) -> String = { it.toString() },
) {

  private val builder = StringBuilder()
  val string: String get() = builder.toString()

  init {
    visit(prefixer.comparator())
  }

  private fun visit(comparator: Comparator<N>? = null) {
    val visiting = linkedMapOf<N, MutableSet<N>>()

    // start by printing root node
    builder.append(prefixer.prefix(root))
    builder.appendReproducibleNewLine(stringify(root))

    fun dfs(node: N) {
      val subs = tree.dominanceGraph.successors(node).run { comparator?.let { sortedWith(it) } ?: this }
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

        builder.append(prefixer.prefix(sub))
        builder.appendReproducibleNewLine(stringify(sub))

        dfs(sub)

        visiting[node]!!.remove(sub)
      }
      visiting.remove(node)
    }

    dfs(root)
  }

  internal interface Prefixer<N : Any> {
    /** A [Comparator] for sorting nodes of type [N]. May be null if you don't care about print order. */
    fun comparator(): Comparator<N>?

    /** When nodes are printed, they may be prefixed arbitrarily.*/
    fun prefix(node: N): String
  }

  private companion object {
    private const val SLASH = """\--- """
    private const val PLUS = "+--- "
    private const val TAB = "     "
    private const val PIPE = "|    "
  }
}
